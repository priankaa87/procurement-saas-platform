package com.procurementsaas.enlistment;

import com.procurementsaas.enlistment.repo.EnlistmentRepository;
import com.procurementsaas.events.SupplierDebarredEvent;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for pre-qualification against real PostgreSQL and Kafka.
 */
// Only a consumer in production; the test stands in for vendor-management.
@SpringBootTest(properties = {
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@AutoConfigureMockMvc
@Testcontainers
class EnlistmentServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

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
    EnlistmentRepository enlistmentRepository;

    private static final String VIEW = "FEATURE_ENLISTMENT_VIEW";
    private static final String MANAGE = "FEATURE_ENLISTMENT_MANAGE";
    private static final String APPLY = "FEATURE_ENLISTMENT_APPLY";
    private static final String ASSESS = "FEATURE_ENLISTMENT_ASSESS";

    private static RequestPostProcessor with(String feature) {
        return jwt().authorities(new SimpleGrantedAuthority(feature));
    }

    /** Creates a DRAFT round whose deadline is {@code secondsAhead} away. */
    private void createSchedule(String code, long secondsAhead, String passMark) throws Exception {
        mvc.perform(post("/enlistment-schedules").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","title":"Round %s","categoryCode":"IT-HARDWARE",
                     "applicationDeadline":"%s","passMark":%s,"validityMonths":12}
                    """.formatted(code, code, Instant.now().plusSeconds(secondsAhead), passMark)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    private void addCriterion(String code, String criterion, int weight, boolean mandatory)
            throws Exception {
        mvc.perform(post("/enlistment-schedules/" + code + "/criteria").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"%s name","weight":%d,"mandatory":%b}
                    """.formatted(criterion, criterion, weight, mandatory)))
            .andExpect(status().isCreated());
    }

    private void apply(String code, String supplier) throws Exception {
        mvc.perform(post("/enlistment-schedules/" + code + "/applications").with(with(APPLY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"" + supplier + "\"}"))
            .andExpect(status().isCreated());
    }

    private void assess(String code, String supplier, String criterion, String score, boolean met)
            throws Exception {
        mvc.perform(put("/enlistment-schedules/" + code + "/applications/" + supplier
                + "/assessments").with(with(ASSESS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"criterionCode":"%s","score":%s,"met":%b,"comment":"ok"}
                    """.formatted(criterion, score, met)))
            .andExpect(status().isOk());
    }

    /** An open round with two criteria (one mandatory), ready for applicants. */
    private void openRound(String code, long secondsAhead) throws Exception {
        createSchedule(code, secondsAhead, "60");
        addCriterion(code, "QUALITY", 70, false);
        addCriterion(code, "LICENCE", 30, true);
        mvc.perform(post("/enlistment-schedules/" + code + "/publish").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"));
    }

    private void closeRound(String code) throws Exception {
        Thread.sleep(2500);   // let the short deadline pass
        mvc.perform(post("/enlistment-schedules/" + code + "/close").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/enlistment-schedules")).andExpect(status().isUnauthorized());
    }

    // --- Round setup ---------------------------------------------------------

    @Test
    void aRoundWithNoCriteriaCannotBePublished() throws Exception {
        createSchedule("E-NOCRIT", 3600, "60");
        mvc.perform(post("/enlistment-schedules/E-NOCRIT/publish").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void criteriaWeightsMustTotalOneHundred() throws Exception {
        createSchedule("E-BADW", 3600, "60");
        addCriterion("E-BADW", "QUALITY", 70, false);   // only 70
        mvc.perform(post("/enlistment-schedules/E-BADW/publish").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    /** Applicants are judged against the published standard, so it cannot move. */
    @Test
    void criteriaAreFixedOncePublished() throws Exception {
        openRound("E-FIXED", 3600);
        mvc.perform(post("/enlistment-schedules/E-FIXED/criteria").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"NEW\",\"name\":\"New\",\"weight\":10,\"mandatory\":false}"))
            .andExpect(status().isConflict());
    }

    // --- Applications --------------------------------------------------------

    @Test
    void applicationsAreRefusedBeforeTheRoundOpens() throws Exception {
        createSchedule("E-DRAFT", 3600, "60");
        addCriterion("E-DRAFT", "QUALITY", 100, false);
        mvc.perform(post("/enlistment-schedules/E-DRAFT/applications").with(with(APPLY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-EARLY\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void applicationsAreRefusedAfterTheDeadline() throws Exception {
        openRound("E-LATE", 2);
        Thread.sleep(2500);
        mvc.perform(post("/enlistment-schedules/E-LATE/applications").with(with(APPLY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-LATE\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void aSupplierMayApplyOnlyOnce() throws Exception {
        openRound("E-ONCE", 3600);
        apply("E-ONCE", "SUP-A");
        mvc.perform(post("/enlistment-schedules/E-ONCE/applications").with(with(APPLY))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\"}"))
            .andExpect(status().isBadRequest());
    }

    // --- Assessment ----------------------------------------------------------

    @Test
    void assessmentIsRefusedWhileApplicationsAreStillArriving() throws Exception {
        openRound("E-EARLY-ASSESS", 3600);
        apply("E-EARLY-ASSESS", "SUP-A");
        mvc.perform(put("/enlistment-schedules/E-EARLY-ASSESS/applications/SUP-A/assessments")
                .with(with(ASSESS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"criterionCode\":\"QUALITY\",\"score\":80,\"met\":true}"))
            .andExpect(status().isConflict());
    }

    @Test
    void aRoundCannotBeClosedBeforeItsDeadline() throws Exception {
        openRound("E-EARLY-CLOSE", 3600);
        mvc.perform(post("/enlistment-schedules/E-EARLY-CLOSE/close").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    /** A decision on half the evidence is a guess, not a decision. */
    @Test
    void anApplicationCannotBeDecidedUntilEveryCriterionIsAssessed() throws Exception {
        openRound("E-PARTIAL", 2);
        apply("E-PARTIAL", "SUP-A");
        closeRound("E-PARTIAL");
        assess("E-PARTIAL", "SUP-A", "QUALITY", "80", true);   // LICENCE not assessed

        mvc.perform(post("/enlistment-schedules/E-PARTIAL/applications/SUP-A/decide")
                .with(with(ASSESS)))
            .andExpect(status().isConflict());
    }

    @Test
    void aStrongApplicantIsQualifiedAndEnlisted() throws Exception {
        openRound("E-PASS", 2);
        apply("E-PASS", "SUP-GOOD");
        closeRound("E-PASS");
        assess("E-PASS", "SUP-GOOD", "QUALITY", "80", true);
        assess("E-PASS", "SUP-GOOD", "LICENCE", "100", true);

        // 80@70 + 100@30 = 86.00
        mvc.perform(post("/enlistment-schedules/E-PASS/applications/SUP-GOOD/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUALIFIED"))
            .andExpect(jsonPath("$.score").value(86.00));

        // Enlisted for the category, valid from today for 12 months.
        mvc.perform(get("/enlistments").param("supplierCode", "SUP-GOOD").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].categoryCode").value("IT-HARDWARE"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[0].currentlyValid").value(true))
            .andExpect(jsonPath("$[0].validUntil").value(LocalDate.now().plusMonths(12).toString()));
    }

    /** An unlicensed supplier is out, however well they score elsewhere. */
    @Test
    void failingAMandatoryCriterionDisqualifiesAndEnlistsNobody() throws Exception {
        openRound("E-MANDATORY", 2);
        apply("E-MANDATORY", "SUP-UNLICENSED");
        closeRound("E-MANDATORY");
        assess("E-MANDATORY", "SUP-UNLICENSED", "QUALITY", "100", true);
        assess("E-MANDATORY", "SUP-UNLICENSED", "LICENCE", "100", false);   // not met

        mvc.perform(post("/enlistment-schedules/E-MANDATORY/applications/SUP-UNLICENSED/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.score").value(100.00))
            .andExpect(jsonPath("$.decisionReason").value(
                org.hamcrest.Matchers.containsString("Mandatory requirement not met")));

        mvc.perform(get("/enlistments").param("supplierCode", "SUP-UNLICENSED").with(with(VIEW)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aWeakApplicantIsRejectedWithTheReason() throws Exception {
        openRound("E-WEAK", 2);
        apply("E-WEAK", "SUP-WEAK");
        closeRound("E-WEAK");
        assess("E-WEAK", "SUP-WEAK", "QUALITY", "30", true);
        assess("E-WEAK", "SUP-WEAK", "LICENCE", "40", true);

        // 30@70 + 40@30 = 33.00, below 60
        mvc.perform(post("/enlistment-schedules/E-WEAK/applications/SUP-WEAK/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.decisionReason").value(
                org.hamcrest.Matchers.containsString("below the pass mark")));
    }

    @Test
    void aRoundCannotCompleteWithApplicationsStillUndecided() throws Exception {
        openRound("E-UNDECIDED", 2);
        apply("E-UNDECIDED", "SUP-A");
        closeRound("E-UNDECIDED");
        mvc.perform(post("/enlistment-schedules/E-UNDECIDED/complete").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    // --- The register --------------------------------------------------------

    @Test
    void theCategoryRegisterListsOnlyCurrentlyValidSuppliers() throws Exception {
        openRound("E-REGISTER", 2);
        apply("E-REGISTER", "SUP-REG");
        closeRound("E-REGISTER");
        assess("E-REGISTER", "SUP-REG", "QUALITY", "90", true);
        assess("E-REGISTER", "SUP-REG", "LICENCE", "100", true);
        mvc.perform(post("/enlistment-schedules/E-REGISTER/applications/SUP-REG/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk());

        mvc.perform(get("/enlistments/qualified").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-REG')]").exists());

        mvc.perform(get("/enlistments/check")
                .param("supplierCode", "SUP-REG").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(jsonPath("$.qualified").value(true));
    }

    @Test
    void aSupplierWhoNeverQualifiedIsNotQualified() throws Exception {
        mvc.perform(get("/enlistments/check")
                .param("supplierCode", "SUP-NEVER").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(jsonPath("$.qualified").value(false));
    }

    // --- Debarment, arriving by event ----------------------------------------

    /**
     * The point of the event backbone: Vendor Management debars a supplier and does not
     * need to know that pre-qualification exists.
     */
    @Test
    void debarringASupplierWithdrawsTheirPreQualifications() throws Exception {
        openRound("E-DEBAR", 2);
        apply("E-DEBAR", "SUP-DEBARRED");
        closeRound("E-DEBAR");
        assess("E-DEBAR", "SUP-DEBARRED", "QUALITY", "90", true);
        assess("E-DEBAR", "SUP-DEBARRED", "LICENCE", "100", true);
        mvc.perform(post("/enlistment-schedules/E-DEBAR/applications/SUP-DEBARRED/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk());

        // Qualified today...
        mvc.perform(get("/enlistments/check")
                .param("supplierCode", "SUP-DEBARRED").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(jsonPath("$.qualified").value(true));

        kafkaTemplate.send(Topics.SUPPLIER_DEBARRED, "SUP-DEBARRED",
            new SupplierDebarredEvent("public", "SUP-DEBARRED", "Dodgy Ltd",
                "Fraudulent documents", LocalDate.now().plusYears(2), Instant.now()));

        // ...and not, once debarred — with the reason kept.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var enlistments = enlistmentRepository.findBySupplierCode("SUP-DEBARRED");
            assertThat(enlistments).hasSize(1);
            assertThat(enlistments.get(0).getStatus().name()).isEqualTo("REVOKED");
            assertThat(enlistments.get(0).isCurrentlyValid()).isFalse();
            assertThat(enlistments.get(0).getRevokedReason()).contains("Fraudulent documents");
        });

        mvc.perform(get("/enlistments/check")
                .param("supplierCode", "SUP-DEBARRED").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(jsonPath("$.qualified").value(false));

        // And they drop out of the register others read to decide who may bid.
        mvc.perform(get("/enlistments/qualified").param("categoryCode", "IT-HARDWARE")
                .with(with(VIEW)))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-DEBARRED')]").doesNotExist());
    }

    @Test
    void aRedeliveredDebarmentIsHarmless() throws Exception {
        openRound("E-DEBAR-DUPE", 2);
        apply("E-DEBAR-DUPE", "SUP-DUPE");
        closeRound("E-DEBAR-DUPE");
        assess("E-DEBAR-DUPE", "SUP-DUPE", "QUALITY", "90", true);
        assess("E-DEBAR-DUPE", "SUP-DUPE", "LICENCE", "100", true);
        mvc.perform(post("/enlistment-schedules/E-DEBAR-DUPE/applications/SUP-DUPE/decide")
                .with(with(ASSESS)))
            .andExpect(status().isOk());

        SupplierDebarredEvent event = new SupplierDebarredEvent("public", "SUP-DUPE", "Dupe Ltd",
            "Late delivery", null, Instant.now());
        kafkaTemplate.send(Topics.SUPPLIER_DEBARRED, "SUP-DUPE", event);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
            assertThat(enlistmentRepository.findBySupplierCode("SUP-DUPE").get(0)
                .getStatus().name()).isEqualTo("REVOKED"));

        // Revoking again finds nothing left to revoke, rather than failing.
        kafkaTemplate.send(Topics.SUPPLIER_DEBARRED, "SUP-DUPE", event);
        await().during(Duration.ofSeconds(4)).atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(enlistmentRepository.findBySupplierCode("SUP-DUPE")).hasSize(1));
    }

    // --- Authorisation -------------------------------------------------------

    @Test
    void assessingRequiresTheCommitteePrivilege() throws Exception {
        openRound("E-PRIV", 2);
        apply("E-PRIV", "SUP-A");
        closeRound("E-PRIV");
        mvc.perform(put("/enlistment-schedules/E-PRIV/applications/SUP-A/assessments")
                .with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"criterionCode\":\"QUALITY\",\"score\":100,\"met\":true}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownScheduleReturns404() throws Exception {
        mvc.perform(get("/enlistment-schedules/E-GHOST").with(with(VIEW)))
            .andExpect(status().isNotFound());
    }
}
