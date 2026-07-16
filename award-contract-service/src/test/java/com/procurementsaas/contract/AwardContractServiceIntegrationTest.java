package com.procurementsaas.contract;

import com.procurementsaas.contract.repo.AwardRepository;
import com.procurementsaas.events.TenderAwardedEvent;
import com.procurementsaas.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the post-award lifecycle against real PostgreSQL and Kafka.
 *
 * <p>The rules worth protecting here are the ones that cost money: no work order without
 * an accepted award, no award accepted after it lapses, and never receiving more than was
 * ordered.
 */
// This service only consumes in production; the test plays the part of tender-service.
@SpringBootTest(properties = {
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@AutoConfigureMockMvc
@Testcontainers
class AwardContractServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    // apache/kafka-native, not confluentinc/cp-kafka: the Confluent image is ~1GB and its
    // first pull dominated the whole CI run. This one is a fraction of the size and starts
    // in about a second.
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.1");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    AwardRepository awardRepository;

    private static final String AWARD_VIEW = "FEATURE_AWARD_VIEW";
    private static final String AWARD_MANAGE = "FEATURE_AWARD_MANAGE";
    private static final String AWARD_RESPOND = "FEATURE_AWARD_RESPOND";
    private static final String CONTRACT_VIEW = "FEATURE_CONTRACT_VIEW";
    private static final String CONTRACT_MANAGE = "FEATURE_CONTRACT_MANAGE";
    private static final String GOODS_RECEIVE = "FEATURE_GOODS_RECEIVE";

    private static RequestPostProcessor with(String feature) {
        return jwt().authorities(new SimpleGrantedAuthority(feature));
    }

    /** Issues an award directly, with an acceptance window that is still open. */
    private void issueAward(String tenderCode, String supplier, String amount) throws Exception {
        issueAward(tenderCode, supplier, amount, LocalDate.now().plusDays(14));
    }

    private void issueAward(String tenderCode, String supplier, String amount, LocalDate respondBy)
            throws Exception {
        mvc.perform(post("/awards").with(with(AWARD_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenderCode":"%s","tenderTitle":"Tender %s","supplierCode":"%s",
                     "amount":%s,"currencyCode":"USD","respondBy":"%s"}
                    """.formatted(tenderCode, tenderCode, supplier, amount, respondBy)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_ACCEPTANCE"));
    }

    private void accept(String tenderCode) throws Exception {
        mvc.perform(post("/awards/" + tenderCode + "/accept").with(with(AWARD_RESPOND)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    private void createWorkOrder(String tenderCode, String code) throws Exception {
        mvc.perform(post("/awards/" + tenderCode + "/work-orders").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    private void addLine(String code, int lineNo, String qty) throws Exception {
        mvc.perform(post("/work-orders/" + code + "/lines").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lineNo":%d,"itemCode":"LAPTOP-01","orderedQuantity":%s,
                     "unitCode":"PCS","dueDate":"%s"}
                    """.formatted(lineNo, qty, LocalDate.now().plusDays(30))))
            .andExpect(status().isCreated());
    }

    private void receive(String code, int lineNo, String qty) throws Exception {
        mvc.perform(post("/work-orders/" + code + "/lines/" + lineNo + "/receipts")
                .with(with(GOODS_RECEIVE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":%s,\"receivedBy\":\"warehouse\",\"remarks\":\"ok\"}"
                    .formatted(qty)))
            .andExpect(status().isCreated());
    }

    /** An accepted award with an issued, single-line work order for 10 units. */
    private void issuedWorkOrder(String tenderCode, String code) throws Exception {
        issueAward(tenderCode, "SUP-A", "5000.00");
        accept(tenderCode);
        createWorkOrder(tenderCode, code);
        addLine(code, 1, "10");
        mvc.perform(post("/work-orders/" + code + "/issue").with(with(CONTRACT_MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/awards")).andExpect(status().isUnauthorized());
    }

    // --- Award acceptance ----------------------------------------------------

    @Test
    void anAwardStartsAwaitingTheSuppliersAnswer() throws Exception {
        issueAward("T-A1", "SUP-A", "1000.00");
        mvc.perform(get("/awards/T-A1").with(with(AWARD_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_ACCEPTANCE"))
            .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    void anAwardCannotBeAnsweredTwice() throws Exception {
        issueAward("T-A2", "SUP-A", "1000.00");
        accept("T-A2");
        mvc.perform(post("/awards/T-A2/accept").with(with(AWARD_RESPOND)))
            .andExpect(status().isConflict());
    }

    @Test
    void aDeclinedAwardRecordsTheReason() throws Exception {
        issueAward("T-A3", "SUP-A", "1000.00");
        mvc.perform(post("/awards/T-A3/decline").with(with(AWARD_RESPOND))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Capacity unavailable\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DECLINED"))
            .andExpect(jsonPath("$.declineReason").value("Capacity unavailable"));
    }

    /** A supplier must not be able to sit on an offer and accept it once terms suit them. */
    @Test
    void anAwardCannotBeAcceptedAfterItsWindowCloses() throws Exception {
        issueAward("T-LAPSED", "SUP-A", "1000.00", LocalDate.now().minusDays(1));
        mvc.perform(post("/awards/T-LAPSED/accept").with(with(AWARD_RESPOND)))
            .andExpect(status().isConflict());
    }

    @Test
    void lapsedAwardsAreExpiredInBulk() throws Exception {
        issueAward("T-EXP-1", "SUP-A", "1000.00", LocalDate.now().minusDays(2));
        issueAward("T-EXP-2", "SUP-B", "2000.00", LocalDate.now().plusDays(5));   // still open

        mvc.perform(post("/awards/expire-lapsed").with(with(AWARD_MANAGE)))
            .andExpect(status().isOk());

        mvc.perform(get("/awards/T-EXP-1").with(with(AWARD_VIEW)))
            .andExpect(jsonPath("$.status").value("EXPIRED"));
        mvc.perform(get("/awards/T-EXP-2").with(with(AWARD_VIEW)))
            .andExpect(jsonPath("$.status").value("PENDING_ACCEPTANCE"));
    }

    @Test
    void answeringAnAwardIsASeparatePrivilegeFromIssuingIt() throws Exception {
        issueAward("T-PRIV", "SUP-A", "1000.00");
        mvc.perform(post("/awards/T-PRIV/accept").with(with(AWARD_MANAGE)))
            .andExpect(status().isForbidden());
    }

    // --- Work orders ---------------------------------------------------------

    @Test
    void aWorkOrderCannotBeRaisedUntilTheAwardIsAccepted() throws Exception {
        issueAward("T-NOACCEPT", "SUP-A", "1000.00");
        mvc.perform(post("/awards/T-NOACCEPT/work-orders").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"WO-NOACCEPT\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void aWorkOrderCannotBeRaisedFromADeclinedAward() throws Exception {
        issueAward("T-DECLINED", "SUP-A", "1000.00");
        mvc.perform(post("/awards/T-DECLINED/decline").with(with(AWARD_RESPOND))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"no\"}"))
            .andExpect(status().isOk());

        mvc.perform(post("/awards/T-DECLINED/work-orders").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"WO-DECLINED\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void aWorkOrderInheritsTheAwardedAmount() throws Exception {
        issueAward("T-AMOUNT", "SUP-A", "7500.50");
        accept("T-AMOUNT");
        mvc.perform(post("/awards/T-AMOUNT/work-orders").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"WO-AMOUNT\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(7500.50))
            .andExpect(jsonPath("$.supplierCode").value("SUP-A"));
    }

    @Test
    void anEmptyWorkOrderCannotBeIssued() throws Exception {
        issueAward("T-EMPTY", "SUP-A", "1000.00");
        accept("T-EMPTY");
        createWorkOrder("T-EMPTY", "WO-EMPTY");
        mvc.perform(post("/work-orders/WO-EMPTY/issue").with(with(CONTRACT_MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void theScheduleIsFixedOnceTheOrderIsIssued() throws Exception {
        issuedWorkOrder("T-FIXED", "WO-FIXED");
        mvc.perform(post("/work-orders/WO-FIXED/lines").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lineNo":2,"itemCode":"X","orderedQuantity":1,"unitCode":"PCS","dueDate":"%s"}
                    """.formatted(LocalDate.now().plusDays(10))))
            .andExpect(status().isConflict());
    }

    // --- Goods receipt -------------------------------------------------------

    @Test
    void aPartialDeliveryLeavesTheLineOutstanding() throws Exception {
        issuedWorkOrder("T-PARTIAL", "WO-PARTIAL");
        receive("WO-PARTIAL", 1, "4");

        mvc.perform(get("/work-orders/WO-PARTIAL/lines").with(with(CONTRACT_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("PARTIAL"))
            .andExpect(jsonPath("$[0].receivedQuantity").value(4.000))
            .andExpect(jsonPath("$[0].outstandingQuantity").value(6.000));

        // The order is under way but not finished.
        mvc.perform(get("/work-orders/WO-PARTIAL").with(with(CONTRACT_VIEW)))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void receiptsAccumulateAndCompleteTheOrderWhenEverythingHasArrived() throws Exception {
        issuedWorkOrder("T-DONE", "WO-DONE");
        receive("WO-DONE", 1, "4");
        receive("WO-DONE", 1, "6");

        mvc.perform(get("/work-orders/WO-DONE/lines").with(with(CONTRACT_VIEW)))
            .andExpect(jsonPath("$[0].status").value("DELIVERED"))
            .andExpect(jsonPath("$[0].outstandingQuantity").value(0.000));

        // Completion is derived from the goods, not from someone remembering to click.
        mvc.perform(get("/work-orders/WO-DONE").with(with(CONTRACT_VIEW)))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completedAt").exists());
    }

    /** Receipts authorise payment, so over-receipting is how you pay for phantom goods. */
    @Test
    void moreGoodsThanOrderedAreRefused() throws Exception {
        issuedWorkOrder("T-OVER", "WO-OVER");
        receive("WO-OVER", 1, "8");

        mvc.perform(post("/work-orders/WO-OVER/lines/1/receipts").with(with(GOODS_RECEIVE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3,\"receivedBy\":\"warehouse\"}"))
            .andExpect(status().isBadRequest());

        // The line is untouched by the rejected receipt.
        mvc.perform(get("/work-orders/WO-OVER/lines").with(with(CONTRACT_VIEW)))
            .andExpect(jsonPath("$[0].receivedQuantity").value(8.000));
    }

    @Test
    void goodsCannotBeReceivedAgainstAnUnissuedOrder() throws Exception {
        issueAward("T-DRAFT", "SUP-A", "1000.00");
        accept("T-DRAFT");
        createWorkOrder("T-DRAFT", "WO-DRAFT");
        addLine("WO-DRAFT", 1, "5");

        mvc.perform(post("/work-orders/WO-DRAFT/lines/1/receipts").with(with(GOODS_RECEIVE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1,\"receivedBy\":\"warehouse\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void receivingGoodsIsASeparatePrivilegeFromManagingTheContract() throws Exception {
        issuedWorkOrder("T-WHPRIV", "WO-WHPRIV");
        mvc.perform(post("/work-orders/WO-WHPRIV/lines/1/receipts").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1,\"receivedBy\":\"nobody\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void everyReceiptIsKeptAsEvidence() throws Exception {
        issuedWorkOrder("T-TRAIL", "WO-TRAIL");
        receive("WO-TRAIL", 1, "3");
        receive("WO-TRAIL", 1, "7");

        mvc.perform(get("/work-orders/WO-TRAIL/lines/1/receipts").with(with(CONTRACT_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].quantity").value(3.000))
            .andExpect(jsonPath("$[1].quantity").value(7.000));
    }

    // --- Delivery extensions -------------------------------------------------

    @Test
    void aDueDateCanBeExtendedButNotBroughtForward() throws Exception {
        issuedWorkOrder("T-EXT", "WO-EXT");
        LocalDate later = LocalDate.now().plusDays(60);

        mvc.perform(put("/work-orders/WO-EXT/lines/1/extend").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"" + later + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dueDate").value(later.toString()));

        mvc.perform(put("/work-orders/WO-EXT/lines/1/extend").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aDeliveredLineCannotBeExtended() throws Exception {
        issuedWorkOrder("T-EXTDONE", "WO-EXTDONE");
        receive("WO-EXTDONE", 1, "10");

        mvc.perform(put("/work-orders/WO-EXTDONE/lines/1/extend").with(with(CONTRACT_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDueDate\":\"" + LocalDate.now().plusDays(90) + "\"}"))
            .andExpect(status().isConflict());
    }

    // --- Event-driven award --------------------------------------------------

    /** A tender awarded elsewhere must raise a notice of award here, with no call. */
    @Test
    void awardingATenderRaisesANoticeOfAwardFromTheEvent() {
        TenderAwardedEvent event = new TenderAwardedEvent("public", "T-EVENT-1", "Servers",
            "SUP-WIN", new BigDecimal("12345.67"), "USD", List.of("SUP-LOSE"), Instant.now());

        kafkaTemplate.send(Topics.TENDER_AWARDED, event.tenderCode(), event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var award = awardRepository.findByTenderCode("T-EVENT-1");
            assertThat(award).isPresent();
            assertThat(award.get().getSupplierCode()).isEqualTo("SUP-WIN");
            // The amount came with the event; nothing was read back out of tender-service.
            assertThat(award.get().getAmount()).isEqualByComparingTo("12345.67");
            assertThat(award.get().getStatus().name()).isEqualTo("PENDING_ACCEPTANCE");
            // The acceptance window was applied.
            assertThat(award.get().getRespondBy()).isAfter(LocalDate.now());
        });
    }

    @Test
    void aRedeliveredAwardEventDoesNotIssueTwoNotices() {
        TenderAwardedEvent event = new TenderAwardedEvent("public", "T-EVENT-DUPE", "Chairs",
            "SUP-DUPE", new BigDecimal("500.00"), "USD", List.of(), Instant.now());

        kafkaTemplate.send(Topics.TENDER_AWARDED, event.tenderCode(), event);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(awardRepository.findByTenderCode("T-EVENT-DUPE")).isPresent());

        kafkaTemplate.send(Topics.TENDER_AWARDED, event.tenderCode(), event);

        await().during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(awardRepository.findAll().stream()
                .filter(a -> a.getTenderCode().equals("T-EVENT-DUPE"))
                .count()).isEqualTo(1));
    }

    @Test
    void aTenderCannotBeAwardedTwice() throws Exception {
        issueAward("T-ONCE", "SUP-A", "1000.00");
        mvc.perform(post("/awards").with(with(AWARD_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenderCode":"T-ONCE","supplierCode":"SUP-B","amount":900,"currencyCode":"USD"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAwardReturns404() throws Exception {
        mvc.perform(get("/awards/T-GHOST").with(with(AWARD_VIEW)))
            .andExpect(status().isNotFound());
    }
}
