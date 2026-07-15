package com.procurementsaas.vendor;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the supplier lifecycle and debarment rules against a real
 * PostgreSQL (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class VendorManagementServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private static RequestPostProcessor view() {
        return jwt().authorities(new SimpleGrantedAuthority("FEATURE_VENDOR_VIEW"));
    }

    private static RequestPostProcessor manage() {
        return jwt().authorities(new SimpleGrantedAuthority("FEATURE_VENDOR_MANAGE"));
    }

    private static RequestPostProcessor debarrer() {
        return jwt().authorities(new SimpleGrantedAuthority("FEATURE_VENDOR_DEBAR"));
    }

    /** Creates a supplier with a unique code and returns its id. */
    private Long createSupplier(String code) throws Exception {
        String body = """
            {"code":"%s","name":"Acme %s","legalName":"Acme Ltd","email":"acme@example.com",
             "phone":"+1000","taxId":"TAX-1","countryIso2":"US","categoryCodes":["IT-HARDWARE"]}
            """.formatted(code, code);
        String json = mvc.perform(post("/suppliers").with(manage())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/suppliers")).andExpect(status().isUnauthorized());
    }

    @Test
    void createRequiresManageAuthority() throws Exception {
        String body = "{\"code\":\"X\",\"name\":\"X\"}";
        mvc.perform(post("/suppliers").with(view())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void newSupplierStartsAsDraftAndCanBeActivated() throws Exception {
        Long id = createSupplier("SUP-ACTIVATE");
        mvc.perform(post("/suppliers/" + id + "/activate").with(manage()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.categoryCodes[0]").value("IT-HARDWARE"));
    }

    @Test
    void debarringBlocksSupplierAndRecordsDecision() throws Exception {
        Long id = createSupplier("SUP-DEBAR");
        mvc.perform(post("/suppliers/" + id + "/activate").with(manage()))
            .andExpect(status().isOk());

        mvc.perform(post("/suppliers/" + id + "/debar").with(debarrer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Fraudulent bid documents\",\"debarredUntil\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reason").value("Fraudulent bid documents"))
            .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/suppliers/" + id).with(view()))
            .andExpect(jsonPath("$.status").value("DEBARRED"));
    }

    @Test
    void debarringRequiresTheDebarFeatureNotJustManage() throws Exception {
        Long id = createSupplier("SUP-PRIV");
        mvc.perform(post("/suppliers/" + id + "/debar").with(manage())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"nope\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void debarredSupplierCannotBeActivatedWithoutReinstatement() throws Exception {
        Long id = createSupplier("SUP-CONFLICT");
        mvc.perform(post("/suppliers/" + id + "/debar").with(debarrer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Blacklisted\"}"))
            .andExpect(status().isOk());

        // Invalid state transition -> 409 Conflict, not 500.
        mvc.perform(post("/suppliers/" + id + "/activate").with(manage()))
            .andExpect(status().isConflict());
    }

    @Test
    void debarringTwiceIsRejected() throws Exception {
        Long id = createSupplier("SUP-TWICE");
        mvc.perform(post("/suppliers/" + id + "/debar").with(debarrer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"First\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/suppliers/" + id + "/debar").with(debarrer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Second\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reinstatementClosesDebarmentAndKeepsHistory() throws Exception {
        Long id = createSupplier("SUP-REINSTATE");
        mvc.perform(post("/suppliers/" + id + "/debar").with(debarrer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Late delivery\"}"))
            .andExpect(status().isOk());

        mvc.perform(post("/suppliers/" + id + "/reinstate").with(debarrer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/suppliers/" + id).with(view()))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // The decision is retained as an audit trail rather than deleted.
        mvc.perform(get("/suppliers/" + id + "/debarments").with(view()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].reason").value("Late delivery"));
    }

    @Test
    void reinstatingWithoutActiveDebarmentIsRejected() throws Exception {
        Long id = createSupplier("SUP-NODEBAR");
        mvc.perform(post("/suppliers/" + id + "/reinstate").with(debarrer()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void contactsAndDocumentsAreScopedToSupplier() throws Exception {
        Long id = createSupplier("SUP-CHILD");
        mvc.perform(post("/suppliers/" + id + "/contacts").with(manage())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jane Roe\",\"email\":\"jane@example.com\",\"phone\":\"+1\",\"primaryContact\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.primaryContact").value(true));

        mvc.perform(post("/suppliers/" + id + "/documents").with(manage())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"docType\":\"TRADE_LICENCE\",\"fileName\":\"lic.pdf\",\"storageKey\":\"s3://docs/lic.pdf\",\"expiresAt\":\"2020-01-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expired").value(true));

        mvc.perform(get("/suppliers/" + id + "/contacts").with(view()))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownSupplierReturns404() throws Exception {
        mvc.perform(get("/suppliers/999999").with(view()))
            .andExpect(status().isNotFound());
    }

    @Test
    void suppliersCanBeFilteredByStatus() throws Exception {
        Long id = createSupplier("SUP-FILTER");
        mvc.perform(post("/suppliers/" + id + "/activate").with(manage()))
            .andExpect(status().isOk());
        mvc.perform(get("/suppliers").param("status", "ACTIVE").with(view()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code=='SUP-FILTER')]").exists());
    }
}
