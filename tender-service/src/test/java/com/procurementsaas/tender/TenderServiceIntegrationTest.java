package com.procurementsaas.tender;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the tender lifecycle against a real PostgreSQL (Testcontainers).
 *
 * <p>The most important tests here are the sealing ones: bids must not be readable before
 * the tender is opened, and a tender must not be openable before its deadline.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private static RequestPostProcessor with(String feature) {
        return jwt().authorities(new SimpleGrantedAuthority(feature));
    }

    private static final String VIEW = "FEATURE_TENDER_VIEW";
    private static final String MANAGE = "FEATURE_TENDER_MANAGE";
    private static final String BID = "FEATURE_TENDER_BID";
    private static final String OPEN = "FEATURE_TENDER_OPEN";
    private static final String EVALUATE = "FEATURE_TENDER_EVALUATE";
    private static final String AWARD = "FEATURE_TENDER_AWARD";

    /** Creates a DRAFT tender whose deadline is {@code secondsAhead} from now. */
    private Long createTender(String code, long secondsAhead) throws Exception {
        String body = """
            {"code":"%s","title":"Laptops for %s","description":"Bulk purchase",
             "currencyCode":"USD","bidDeadline":"%s"}
            """.formatted(code, code, Instant.now().plusSeconds(secondsAhead).toString());
        String json = mvc.perform(post("/tenders").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void addItem(Long tenderId) throws Exception {
        mvc.perform(post("/tenders/" + tenderId + "/items").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemCode\":\"LAPTOP-01\",\"description\":\"14 inch\",\"quantity\":10,\"unitCode\":\"PCS\"}"))
            .andExpect(status().isCreated());
    }

    private void invite(Long tenderId, String supplierCode) throws Exception {
        mvc.perform(post("/tenders/" + tenderId + "/participants").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"" + supplierCode + "\"}"))
            .andExpect(status().isCreated());
    }

    private void submitBid(Long tenderId, String supplierCode, String amount) throws Exception {
        mvc.perform(post("/tenders/" + tenderId + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"" + supplierCode + "\",\"totalAmount\":" + amount + ",\"notes\":\"n/a\"}"))
            .andExpect(status().isCreated());
    }

    /** Creates a published tender with one item and two invited suppliers. */
    private Long publishedTender(String code, long secondsAhead) throws Exception {
        Long id = createTender(code, secondsAhead);
        addItem(id);
        invite(id, "SUP-A");
        invite(id, "SUP-B");
        mvc.perform(post("/tenders/" + id + "/publish").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
        return id;
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/tenders")).andExpect(status().isUnauthorized());
    }

    @Test
    void emptyTenderCannotBePublished() throws Exception {
        Long id = createTender("T-EMPTY", 3600);
        mvc.perform(post("/tenders/" + id + "/publish").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void itemsCannotBeAddedAfterPublication() throws Exception {
        Long id = publishedTender("T-LOCKED", 3600);
        mvc.perform(post("/tenders/" + id + "/items").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemCode\":\"X\",\"description\":\"late\",\"quantity\":1,\"unitCode\":\"PCS\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void bidsAreRejectedBeforePublication() throws Exception {
        Long id = createTender("T-DRAFT-BID", 3600);
        addItem(id);
        invite(id, "SUP-A");
        mvc.perform(post("/tenders/" + id + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"totalAmount\":100}"))
            .andExpect(status().isConflict());
    }

    @Test
    void onlyInvitedSuppliersMayBid() throws Exception {
        Long id = publishedTender("T-UNINVITED", 3600);
        mvc.perform(post("/tenders/" + id + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-STRANGER\",\"totalAmount\":100}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aSupplierMayOnlyBidOnce() throws Exception {
        Long id = publishedTender("T-DUP", 3600);
        submitBid(id, "SUP-A", "100.00");
        mvc.perform(post("/tenders/" + id + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"totalAmount\":90}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void submissionReturnsAReceiptAndNeverEchoesTheAmount() throws Exception {
        Long id = publishedTender("T-RECEIPT", 3600);
        mvc.perform(post("/tenders/" + id + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"totalAmount\":1234.56,\"notes\":\"secret\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bidId").exists())
            .andExpect(jsonPath("$.supplierCode").value("SUP-A"))
            .andExpect(jsonPath("$.totalAmount").doesNotExist())
            .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void bidsStaySealedUntilTheTenderIsOpened() throws Exception {
        Long id = publishedTender("T-SEALED", 3600);
        submitBid(id, "SUP-A", "100.00");

        // Even a user holding the evaluation privilege cannot read bids early.
        mvc.perform(get("/tenders/" + id + "/bids").with(with(EVALUATE)))
            .andExpect(status().isConflict());
    }

    @Test
    void bidCountIsVisibleWhileSealedBecauseItRevealsNoContent() throws Exception {
        Long id = publishedTender("T-COUNT", 3600);
        submitBid(id, "SUP-A", "100.00");
        submitBid(id, "SUP-B", "200.00");

        mvc.perform(get("/tenders/" + id + "/bids/count").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bidCount").value(2));
    }

    @Test
    void tenderCannotBeOpenedBeforeItsDeadline() throws Exception {
        Long id = publishedTender("T-EARLY-OPEN", 3600);
        mvc.perform(post("/tenders/" + id + "/open").with(with(OPEN)))
            .andExpect(status().isConflict());
    }

    @Test
    void tenderCannotBeAwardedBeforeItIsOpened() throws Exception {
        Long id = publishedTender("T-EARLY-AWARD", 3600);
        submitBid(id, "SUP-A", "100.00");
        mvc.perform(post("/tenders/" + id + "/award").with(with(AWARD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void openingAfterTheDeadlineDisclosesBidsCheapestFirst() throws Exception {
        Long id = publishedTender("T-OPEN", 2);
        submitBid(id, "SUP-A", "500.00");
        submitBid(id, "SUP-B", "300.00");

        awaitDeadline();

        mvc.perform(post("/tenders/" + id + "/open").with(with(OPEN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_EVALUATION"))
            .andExpect(jsonPath("$.openedAt").exists());

        mvc.perform(get("/tenders/" + id + "/bids").with(with(EVALUATE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].supplierCode").value("SUP-B"))
            .andExpect(jsonPath("$[0].totalAmount").value(300.00))
            .andExpect(jsonPath("$[1].supplierCode").value("SUP-A"));
    }

    @Test
    void bidsAreRejectedAfterTheDeadlinePasses() throws Exception {
        Long id = publishedTender("T-LATE", 2);
        awaitDeadline();
        mvc.perform(post("/tenders/" + id + "/bids").with(with(BID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"totalAmount\":100}"))
            .andExpect(status().isConflict());
    }

    @Test
    void awardGoesToABidderAndClosesTheTender() throws Exception {
        Long id = publishedTender("T-AWARD", 2);
        submitBid(id, "SUP-A", "400.00");
        awaitDeadline();
        mvc.perform(post("/tenders/" + id + "/open").with(with(OPEN)))
            .andExpect(status().isOk());

        mvc.perform(post("/tenders/" + id + "/award").with(with(AWARD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AWARDED"))
            .andExpect(jsonPath("$.awardedSupplierCode").value("SUP-A"));
    }

    @Test
    void aTenderCannotBeAwardedToSomeoneWhoDidNotBid() throws Exception {
        Long id = publishedTender("T-NOBID-AWARD", 2);
        submitBid(id, "SUP-A", "400.00");
        awaitDeadline();
        mvc.perform(post("/tenders/" + id + "/open").with(with(OPEN)))
            .andExpect(status().isOk());

        mvc.perform(post("/tenders/" + id + "/award").with(with(AWARD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-B\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void openingRequiresItsOwnPrivilege() throws Exception {
        Long id = publishedTender("T-PRIV", 3600);
        mvc.perform(post("/tenders/" + id + "/open").with(with(MANAGE)))
            .andExpect(status().isForbidden());
    }

    @Test
    void participantIsMarkedOnceTheyBid() throws Exception {
        Long id = publishedTender("T-PARTICIPANT", 3600);
        submitBid(id, "SUP-A", "100.00");
        mvc.perform(get("/tenders/" + id + "/participants").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-A')].status").value("BID_SUBMITTED"))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-B')].status").value("INVITED"));
    }

    @Test
    void unknownTenderReturns404() throws Exception {
        mvc.perform(get("/tenders/999999").with(with(VIEW)))
            .andExpect(status().isNotFound());
    }

    /** Waits out the short deadline used by the opening tests. */
    private static void awaitDeadline() throws InterruptedException {
        Thread.sleep(2500);
    }
}
