package com.procurementsaas.evaluation;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the two-stage evaluation against a real PostgreSQL.
 *
 * <p>The rules that matter: technical scoring closes before any price is scored, a
 * half-scored evaluation cannot be closed, participants below the pass mark drop out, and
 * the comparative statement stays unavailable until the whole thing is finished.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EvaluationServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String VIEW = "FEATURE_EVAL_VIEW";
    private static final String MANAGE = "FEATURE_EVAL_MANAGE";
    private static final String SCORE = "FEATURE_EVAL_SCORE";

    private static RequestPostProcessor with(String feature) {
        return jwt().authorities(new SimpleGrantedAuthority(feature));
    }

    private Long createEvaluation(String tenderCode, int techWeight, int finWeight, String passMark)
            throws Exception {
        String body = """
            {"tenderCode":"%s","technicalWeight":%d,"financialWeight":%d,"passMark":%s}
            """.formatted(tenderCode, techWeight, finWeight, passMark);
        String json = mvc.perform(post("/evaluations").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void addCriterion(Long id, String code, int weight) throws Exception {
        mvc.perform(post("/evaluations/" + id + "/criteria").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"%s\",\"name\":\"%s\",\"weight\":%d}".formatted(code, code, weight)))
            .andExpect(status().isCreated());
    }

    private void addParticipant(Long id, String supplier, String bid) throws Exception {
        mvc.perform(post("/evaluations/" + id + "/participants").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"%s\",\"bidAmount\":%s}".formatted(supplier, bid)))
            .andExpect(status().isCreated());
    }

    private void score(Long id, String supplier, String criterion, String value) throws Exception {
        mvc.perform(put("/evaluations/" + id + "/scores").with(with(SCORE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"%s\",\"criterionCode\":\"%s\",\"score\":%s,\"comment\":\"ok\"}"
                    .formatted(supplier, criterion, value)))
            .andExpect(status().isOk());
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/evaluations/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void stageWeightsMustTotalOneHundred() throws Exception {
        mvc.perform(post("/evaluations").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenderCode\":\"T-BADW\",\"technicalWeight\":70,\"financialWeight\":20,\"passMark\":50}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void criteriaWeightsMustTotalOneHundredBeforeTechnicalCanClose() throws Exception {
        Long id = createEvaluation("T-BADCW", 70, 30, "50");
        addCriterion(id, "QUALITY", 60);   // only 60 in total
        addParticipant(id, "SUP-A", "100");
        score(id, "SUP-A", "QUALITY", "80");

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void technicalCannotCloseWhileAnyParticipantIsUnscored() throws Exception {
        Long id = createEvaluation("T-PARTIAL", 70, 30, "50");
        addCriterion(id, "QUALITY", 50);
        addCriterion(id, "DELIVERY", 50);
        addParticipant(id, "SUP-A", "100");
        addParticipant(id, "SUP-B", "120");
        score(id, "SUP-A", "QUALITY", "80");
        score(id, "SUP-A", "DELIVERY", "70");
        score(id, "SUP-B", "QUALITY", "60");
        // SUP-B has no DELIVERY score

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void scoresOutsideZeroToOneHundredAreRejected() throws Exception {
        Long id = createEvaluation("T-RANGE", 70, 30, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");

        mvc.perform(put("/evaluations/" + id + "/scores").with(with(SCORE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"criterionCode\":\"QUALITY\",\"score\":150}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void financialStageCannotRunBeforeTechnicalCloses() throws Exception {
        Long id = createEvaluation("T-SEQ", 70, 30, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");
        score(id, "SUP-A", "QUALITY", "80");

        mvc.perform(post("/evaluations/" + id + "/complete").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void scoresAreFrozenOnceTechnicalCloses() throws Exception {
        Long id = createEvaluation("T-FROZEN", 70, 30, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");
        score(id, "SUP-A", "QUALITY", "80");
        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk());

        mvc.perform(put("/evaluations/" + id + "/scores").with(with(SCORE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"criterionCode\":\"QUALITY\",\"score\":95}"))
            .andExpect(status().isConflict());
    }

    @Test
    void participantsBelowThePassMarkAreDisqualified() throws Exception {
        Long id = createEvaluation("T-PASSMARK", 70, 30, "60");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-GOOD", "100");
        addParticipant(id, "SUP-WEAK", "50");
        score(id, "SUP-GOOD", "QUALITY", "80");
        score(id, "SUP-WEAK", "QUALITY", "40");   // below the 60 pass mark

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-GOOD')].qualified").value(true))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-WEAK')].qualified").value(false));
    }

    @Test
    void aCheapButTechnicallyFailedBidNeverWins() throws Exception {
        Long id = createEvaluation("T-CHEAPFAIL", 70, 30, "60");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-GOOD", "1000");
        addParticipant(id, "SUP-CHEAP", "10");    // by far the cheapest
        score(id, "SUP-GOOD", "QUALITY", "90");
        score(id, "SUP-CHEAP", "QUALITY", "20");  // but technically unacceptable

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk());
        mvc.perform(post("/evaluations/" + id + "/complete").with(with(MANAGE)))
            .andExpect(status().isOk());

        // The cheap bidder is unranked and unpriced; the qualified bidder wins by default.
        mvc.perform(get("/evaluations/" + id + "/comparative-statement").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendedSupplierCode").value("SUP-GOOD"))
            .andExpect(jsonPath("$.rows[?(@.supplierCode=='SUP-CHEAP')].rank").value((Object) null))
            .andExpect(jsonPath("$.rows[?(@.supplierCode=='SUP-CHEAP')].financialScore").value((Object) null));
    }

    @Test
    void completeEvaluationScoresRanksAndRecommends() throws Exception {
        Long id = createEvaluation("T-FULL", 70, 30, "50");
        addCriterion(id, "QUALITY", 60);
        addCriterion(id, "DELIVERY", 40);
        addParticipant(id, "SUP-A", "600");   // pricier
        addParticipant(id, "SUP-B", "300");   // cheapest

        // SUP-A technical: 80*60 + 90*40 = 84.00 ; SUP-B: 60*60 + 50*40 = 56.00
        score(id, "SUP-A", "QUALITY", "80");
        score(id, "SUP-A", "DELIVERY", "90");
        score(id, "SUP-B", "QUALITY", "60");
        score(id, "SUP-B", "DELIVERY", "50");

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-A')].technicalScore").value(84.00))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-B')].technicalScore").value(56.00));

        // Financial: SUP-B lowest -> 100 ; SUP-A -> 300/600*100 = 50
        // Combined:  SUP-A = 84*0.7 + 50*0.3 = 73.80 ; SUP-B = 56*0.7 + 100*0.3 = 69.20
        mvc.perform(post("/evaluations/" + id + "/complete").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-B')].financialScore").value(100.00))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-A')].financialScore").value(50.00))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-A')].combinedScore").value(73.80))
            .andExpect(jsonPath("$[?(@.supplierCode=='SUP-B')].combinedScore").value(69.20));

        // The better technical bid wins despite being twice the price.
        mvc.perform(get("/evaluations/" + id + "/comparative-statement").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendedSupplierCode").value("SUP-A"))
            .andExpect(jsonPath("$.rows[0].rank").value(1))
            .andExpect(jsonPath("$.rows[0].supplierCode").value("SUP-A"))
            .andExpect(jsonPath("$.rows[1].rank").value(2))
            .andExpect(jsonPath("$.rows[1].supplierCode").value("SUP-B"));
    }

    @Test
    void priceWinsWhenTechnicalScoresTieAndPriceIsWeightedHeavily() throws Exception {
        Long id = createEvaluation("T-PRICEWINS", 30, 70, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "1000");
        addParticipant(id, "SUP-B", "500");
        score(id, "SUP-A", "QUALITY", "80");
        score(id, "SUP-B", "QUALITY", "80");

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk());
        mvc.perform(post("/evaluations/" + id + "/complete").with(with(MANAGE)))
            .andExpect(status().isOk());

        mvc.perform(get("/evaluations/" + id + "/comparative-statement").with(with(VIEW)))
            .andExpect(jsonPath("$.recommendedSupplierCode").value("SUP-B"));
    }

    @Test
    void comparativeStatementIsUnavailableUntilTheEvaluationIsComplete() throws Exception {
        Long id = createEvaluation("T-EARLY-CS", 70, 30, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");
        score(id, "SUP-A", "QUALITY", "80");

        mvc.perform(get("/evaluations/" + id + "/comparative-statement").with(with(VIEW)))
            .andExpect(status().isConflict());

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk());

        // Still not complete: financial stage has not run.
        mvc.perform(get("/evaluations/" + id + "/comparative-statement").with(with(VIEW)))
            .andExpect(status().isConflict());
    }

    @Test
    void anEvaluationWhereNobodyQualifiesCannotBeCompleted() throws Exception {
        Long id = createEvaluation("T-NOQUAL", 70, 30, "70");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");
        score(id, "SUP-A", "QUALITY", "40");

        mvc.perform(post("/evaluations/" + id + "/close-technical").with(with(MANAGE)))
            .andExpect(status().isOk());
        mvc.perform(post("/evaluations/" + id + "/complete").with(with(MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void scoringRequiresItsOwnPrivilege() throws Exception {
        Long id = createEvaluation("T-PRIV", 70, 30, "50");
        addCriterion(id, "QUALITY", 100);
        addParticipant(id, "SUP-A", "100");

        mvc.perform(put("/evaluations/" + id + "/scores").with(with(VIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierCode\":\"SUP-A\",\"criterionCode\":\"QUALITY\",\"score\":80}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void oneEvaluationPerTender() throws Exception {
        createEvaluation("T-UNIQUE", 70, 30, "50");
        mvc.perform(post("/evaluations").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenderCode\":\"T-UNIQUE\",\"technicalWeight\":70,\"financialWeight\":30,\"passMark\":50}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEvaluationReturns404() throws Exception {
        mvc.perform(get("/evaluations/999999").with(with(VIEW)))
            .andExpect(status().isNotFound());
    }
}
