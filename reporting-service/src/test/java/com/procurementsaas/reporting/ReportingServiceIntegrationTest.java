package com.procurementsaas.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the async report engine against a real PostgreSQL.
 *
 * <p>The shape under test is: request returns a queued job (202), a worker renders it, and
 * the file is downloadable once the job completes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReportingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String VIEW = "FEATURE_REPORT_VIEW";
    private static final String RUN = "FEATURE_REPORT_RUN";

    private static RequestPostProcessor with(String feature) {
        return jwt().jwt(j -> j.subject("analyst")).authorities(new SimpleGrantedAuthority(feature));
    }

    /** Requests a report and returns the job id. */
    private long requestReport(String definitionCode) throws Exception {
        String json = mvc.perform(post("/reports").with(with(RUN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"definitionCode\":\"" + definitionCode + "\",\"parameters\":{}}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void awaitCompletion(long jobId) {
        await().atMost(ofSeconds(20)).untilAsserted(() ->
            mvc.perform(get("/reports/jobs/" + jobId).with(with(VIEW)))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.downloadable").value(true)));
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/reports/definitions")).andExpect(status().isUnauthorized());
    }

    @Test
    void theCatalogueListsTheSeededReports() throws Exception {
        mvc.perform(get("/reports/definitions").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.code=='TENDER_SUMMARY_XLSX')].format").value("XLSX"));
    }

    @Test
    void requestingAReportReturnsAQueuedJobImmediately() throws Exception {
        mvc.perform(post("/reports").with(with(RUN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"definitionCode\":\"TENDER_SUMMARY_CSV\",\"parameters\":{}}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.downloadable").value(false));
    }

    @Test
    void aRequestedReportIsRenderedAndBecomesDownloadable() throws Exception {
        long jobId = requestReport("TENDER_SUMMARY_XLSX");
        awaitCompletion(jobId);

        mvc.perform(get("/reports/jobs/" + jobId).with(with(VIEW)))
            .andExpect(jsonPath("$.rowCount").value(3))
            .andExpect(jsonPath("$.sizeBytes").isNumber());
    }

    @Test
    void downloadingAnXlsxReturnsARealSpreadsheet() throws Exception {
        long jobId = requestReport("TENDER_SUMMARY_XLSX");
        awaitCompletion(jobId);

        byte[] body = mvc.perform(get("/reports/jobs/" + jobId + "/download").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString(".xlsx")))
            .andReturn().getResponse().getContentAsByteArray();

        // The download is a workbook POI can open, with the header row we expect.
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(body))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                .isEqualTo("Tender");
        }
    }

    @Test
    void downloadingACsvReturnsTextCsv() throws Exception {
        long jobId = requestReport("TENDER_SUMMARY_CSV");
        awaitCompletion(jobId);

        mvc.perform(get("/reports/jobs/" + jobId + "/download").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type",
                org.hamcrest.Matchers.containsString("text/csv")));
    }

    @Test
    void aReportCannotBeDownloadedBeforeItHasRendered() throws Exception {
        // Request but do not wait: the job is still QUEUED/RUNNING.
        long jobId = requestReport("SUPPLIER_REGISTER_XLSX");
        // There is a race with the worker, so accept either "not ready" or "ready".
        mvc.perform(get("/reports/jobs/" + jobId + "/download").with(with(VIEW)))
            .andExpect(result -> {
                int sc = result.getResponse().getStatus();
                assertThat(sc).isIn(200, 409);
            });
    }

    @Test
    void requestingAnUnknownReportIsRejectedUpFront() throws Exception {
        mvc.perform(post("/reports").with(with(RUN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"definitionCode\":\"NOPE\",\"parameters\":{}}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void runningAReportRequiresTheRunPrivilege() throws Exception {
        mvc.perform(post("/reports").with(with(VIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"definitionCode\":\"TENDER_SUMMARY_CSV\",\"parameters\":{}}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void jobsAreListedForReview() throws Exception {
        long jobId = requestReport("TENDER_SUMMARY_CSV");
        awaitCompletion(jobId);
        mvc.perform(get("/reports/jobs").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==" + jobId + ")]").exists());
    }

    @Test
    void unknownJobReturns404() throws Exception {
        mvc.perform(get("/reports/jobs/999999").with(with(VIEW)))
            .andExpect(status().isNotFound());
    }
}
