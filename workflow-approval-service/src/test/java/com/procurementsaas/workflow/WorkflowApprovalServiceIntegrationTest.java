package com.procurementsaas.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the approval engine against a real PostgreSQL.
 *
 * <p>Most of these exist to pin down two controls that are easy to state and easy to lose:
 * nobody approves their own request, and delegated authority is bounded and attributable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WorkflowApprovalServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // Endpoint-level privileges.
    private static final String WF_VIEW = "FEATURE_WORKFLOW_VIEW";
    private static final String WF_MANAGE = "FEATURE_WORKFLOW_MANAGE";
    private static final String AP_VIEW = "FEATURE_APPROVAL_VIEW";
    private static final String AP_REQUEST = "FEATURE_APPROVAL_REQUEST";
    private static final String AP_ACT = "FEATURE_APPROVAL_ACT";
    private static final String DEL_MANAGE = "FEATURE_DELEGATION_MANAGE";

    // Business roles, as they arrive from Keycloak.
    private static final String ROLE_PROCUREMENT = "ROLE_PROCUREMENT_OFFICER";
    private static final String ROLE_FINANCE = "ROLE_FINANCE_MANAGER";

    /** Authenticates as {@code user}, holding exactly the given authorities. */
    private static RequestPostProcessor as(String user, String... authorities) {
        List<GrantedAuthority> granted = Arrays.stream(authorities)
            .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
            .toList();
        return jwt().jwt(j -> j.subject(user)).authorities(granted);
    }

    /** Raises a TENDER_PUBLISH approval for a tender, requested by {@code requester}. */
    private Long startRequest(String tenderCode, String requester) throws Exception {
        String json = mvc.perform(post("/approvals").with(as(requester, AP_REQUEST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"workflowCode":"TENDER_PUBLISH","subjectRef":"%s","reason":"Ready to publish"}
                    """.formatted(tenderCode)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/workflows")).andExpect(status().isUnauthorized());
    }

    @Test
    void theSeededWorkflowCarriesItsStepsInOrder() throws Exception {
        mvc.perform(get("/workflows/TENDER_PUBLISH").with(as("admin", WF_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subjectType").value("TENDER"))
            .andExpect(jsonPath("$.steps.length()").value(2))
            .andExpect(jsonPath("$.steps[0].roleCode").value("PROCUREMENT_OFFICER"))
            .andExpect(jsonPath("$.steps[1].roleCode").value("FINANCE_MANAGER"));
    }

    // --- Request lifecycle ---------------------------------------------------

    @Test
    void aNewRequestWaitsOnTheFirstStepsRole() throws Exception {
        Long id = startRequest("T-100", "alice");
        mvc.perform(get("/approvals/" + id).with(as("anyone", AP_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStep").value(1))
            .andExpect(jsonPath("$.totalSteps").value(2))
            .andExpect(jsonPath("$.currentRoleCode").value("PROCUREMENT_OFFICER"));
    }

    @Test
    void twoOpenRequestsForTheSameSubjectAreRefused() throws Exception {
        startRequest("T-DUPE", "alice");
        mvc.perform(post("/approvals").with(as("bob", AP_REQUEST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowCode\":\"TENDER_PUBLISH\",\"subjectRef\":\"T-DUPE\"}"))
            .andExpect(status().isBadRequest());
    }

    /** The whole point of an approval: the person asking cannot be the person agreeing. */
    @Test
    void nobodyMayApproveTheirOwnRequest() throws Exception {
        Long id = startRequest("T-SELF", "alice");

        // Alice holds exactly the role the step needs — and is still refused.
        mvc.perform(post("/approvals/" + id + "/approve")
                .with(as("alice", AP_ACT, ROLE_PROCUREMENT)))
            .andExpect(status().isConflict());
    }

    @Test
    void nobodyMayRejectTheirOwnRequestEither() throws Exception {
        Long id = startRequest("T-SELFREJ", "alice");
        mvc.perform(post("/approvals/" + id + "/reject")
                .with(as("alice", AP_ACT, ROLE_PROCUREMENT)))
            .andExpect(status().isConflict());
    }

    @Test
    void someoneWithoutTheStepsRoleCannotDecideIt() throws Exception {
        Long id = startRequest("T-NOROLE", "alice");
        // Bob may take part in approvals, but is not a procurement officer.
        mvc.perform(post("/approvals/" + id + "/approve").with(as("bob", AP_ACT)))
            .andExpect(status().isForbidden());
    }

    @Test
    void theRoleForALaterStepDoesNotUnlockAnEarlierOne() throws Exception {
        Long id = startRequest("T-ORDER", "alice");
        // Finance is step 2; they cannot reach past procurement to approve step 1.
        mvc.perform(post("/approvals/" + id + "/approve").with(as("fin", AP_ACT, ROLE_FINANCE)))
            .andExpect(status().isForbidden());
    }

    @Test
    void approvingAStepHandsTheRequestToTheNextRole() throws Exception {
        Long id = startRequest("T-ADVANCE", "alice");
        mvc.perform(post("/approvals/" + id + "/approve").with(as("proc", AP_ACT, ROLE_PROCUREMENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Scope looks right\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.currentStep").value(2))
            .andExpect(jsonPath("$.currentRoleCode").value("FINANCE_MANAGER"));
    }

    @Test
    void approvingTheLastStepCompletesTheRequest() throws Exception {
        Long id = startRequest("T-COMPLETE", "alice");
        mvc.perform(post("/approvals/" + id + "/approve").with(as("proc", AP_ACT, ROLE_PROCUREMENT)))
            .andExpect(status().isOk());
        mvc.perform(post("/approvals/" + id + "/approve").with(as("fin", AP_ACT, ROLE_FINANCE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.completedAt").exists());
    }

    /** An approval chain is a series of vetoes, not a vote. */
    @Test
    void aSingleRejectionEndsTheRequestEvenAtTheFirstStep() throws Exception {
        Long id = startRequest("T-REJECT", "alice");
        mvc.perform(post("/approvals/" + id + "/reject").with(as("proc", AP_ACT, ROLE_PROCUREMENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Budget not secured\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        // Finance never gets a say.
        mvc.perform(post("/approvals/" + id + "/approve").with(as("fin", AP_ACT, ROLE_FINANCE)))
            .andExpect(status().isConflict());
    }

    @Test
    void onlyTheRequesterMayWithdrawTheirRequest() throws Exception {
        Long id = startRequest("T-CANCEL", "alice");
        mvc.perform(post("/approvals/" + id + "/cancel").with(as("mallory", AP_REQUEST)))
            .andExpect(status().isConflict());
        mvc.perform(post("/approvals/" + id + "/cancel").with(as("alice", AP_REQUEST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // --- Audit trail ---------------------------------------------------------

    @Test
    void aDirectApprovalIsRecordedAgainstTheApproverAlone() throws Exception {
        Long id = startRequest("T-TRAIL", "alice");
        mvc.perform(post("/approvals/" + id + "/approve").with(as("proc", AP_ACT, ROLE_PROCUREMENT))
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"fine\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/approvals/" + id + "/history").with(as("auditor", AP_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].actorId").value("proc"))
            .andExpect(jsonPath("$[0].delegated").value(false))
            .andExpect(jsonPath("$[0].onBehalfOf").doesNotExist())
            .andExpect(jsonPath("$[0].comment").value("fine"));
    }

    // --- Delegation ----------------------------------------------------------

    private void delegate(String from, String to, String roleCode,
                          LocalDate validFrom, LocalDate validTo) throws Exception {
        mvc.perform(post("/delegations").with(as(from, DEL_MANAGE, "ROLE_" + roleCode))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"toUser":"%s","roleCode":"%s","validFrom":"%s","validTo":"%s","reason":"Leave"}
                    """.formatted(to, roleCode, validFrom, validTo)))
            .andExpect(status().isCreated());
    }

    /** A stand-in may act — and the record shows both of them. */
    @Test
    void aDelegateMayActAndTheTrailNamesBothPeople() throws Exception {
        Long id = startRequest("T-DELEGATE", "alice");
        delegate("proc_boss", "deputy", "PROCUREMENT_OFFICER",
            LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

        // The deputy does not hold PROCUREMENT_OFFICER themselves.
        mvc.perform(post("/approvals/" + id + "/approve").with(as("deputy", AP_ACT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Covering while away\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStep").value(2));

        // Who acted, and whose authority they used — both, not one.
        mvc.perform(get("/approvals/" + id + "/history").with(as("auditor", AP_VIEW)))
            .andExpect(jsonPath("$[0].actorId").value("deputy"))
            .andExpect(jsonPath("$[0].onBehalfOf").value("proc_boss"))
            .andExpect(jsonPath("$[0].delegated").value(true))
            .andExpect(jsonPath("$[0].roleCode").value("PROCUREMENT_OFFICER"));
    }

    /** A delegation that has run out must stop working on its own. */
    @Test
    void anExpiredDelegationGrantsNothing() throws Exception {
        Long id = startRequest("T-EXPIRED-DEL", "alice");
        delegate("proc_boss", "old_deputy", "PROCUREMENT_OFFICER",
            LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));

        mvc.perform(post("/approvals/" + id + "/approve").with(as("old_deputy", AP_ACT)))
            .andExpect(status().isForbidden());
    }

    @Test
    void aDelegationThatHasNotStartedYetGrantsNothing() throws Exception {
        Long id = startRequest("T-FUTURE-DEL", "alice");
        delegate("proc_boss", "future_deputy", "PROCUREMENT_OFFICER",
            LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

        mvc.perform(post("/approvals/" + id + "/approve").with(as("future_deputy", AP_ACT)))
            .andExpect(status().isForbidden());
    }

    @Test
    void revokingADelegationStopsTheDelegateActing() throws Exception {
        delegate("proc_boss", "revoked_deputy", "PROCUREMENT_OFFICER",
            LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

        String json = mvc.perform(get("/delegations/received").with(as("revoked_deputy", DEL_MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].currentlyValid").value(true))
            .andReturn().getResponse().getContentAsString();
        long delegationId = objectMapper.readTree(json).get(0).get("id").asLong();

        mvc.perform(post("/delegations/" + delegationId + "/revoke").with(as("proc_boss", DEL_MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revoked").value(true));

        Long id = startRequest("T-REVOKED-DEL", "alice");
        mvc.perform(post("/approvals/" + id + "/approve").with(as("revoked_deputy", AP_ACT)))
            .andExpect(status().isForbidden());
    }

    /** Otherwise delegation is a way to conjure a role you were never given. */
    @Test
    void youCannotDelegateARoleYouDoNotHold() throws Exception {
        mvc.perform(post("/delegations").with(as("nobody", DEL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"toUser":"friend","roleCode":"FINANCE_MANAGER","validFrom":"%s","validTo":"%s"}
                    """.formatted(LocalDate.now(), LocalDate.now().plusDays(5))))
            .andExpect(status().isConflict());
    }

    @Test
    void onlyTheDelegatorMayTakeTheAuthorityBack() throws Exception {
        delegate("fin_boss", "fin_deputy", "FINANCE_MANAGER",
            LocalDate.now(), LocalDate.now().plusDays(5));

        String json = mvc.perform(get("/delegations/received").with(as("fin_deputy", DEL_MANAGE)))
            .andReturn().getResponse().getContentAsString();
        long delegationId = objectMapper.readTree(json).get(0).get("id").asLong();

        mvc.perform(post("/delegations/" + delegationId + "/revoke").with(as("stranger", DEL_MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void aUserCannotDelegateToThemselves() throws Exception {
        mvc.perform(post("/delegations").with(as("solo", DEL_MANAGE, ROLE_FINANCE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"toUser":"solo","roleCode":"FINANCE_MANAGER","validFrom":"%s","validTo":"%s"}
                    """.formatted(LocalDate.now(), LocalDate.now().plusDays(5))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aDelegationCannotEndBeforeItStarts() throws Exception {
        mvc.perform(post("/delegations").with(as("fin_boss2", DEL_MANAGE, ROLE_FINANCE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"toUser":"someone","roleCode":"FINANCE_MANAGER","validFrom":"%s","validTo":"%s"}
                    """.formatted(LocalDate.now(), LocalDate.now().minusDays(1))))
            .andExpect(status().isBadRequest());
    }

    // --- Configuration -------------------------------------------------------

    @Test
    void aNewWorkflowCanBeConfiguredAndUsed() throws Exception {
        mvc.perform(post("/workflows").with(as("admin", WF_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"CONTRACT_VARY","name":"Contract variation","subjectType":"CONTRACT"}
                    """))
            .andExpect(status().isCreated());
        mvc.perform(post("/workflows/CONTRACT_VARY/steps").with(as("admin", WF_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepNo\":1,\"name\":\"Legal review\",\"roleCode\":\"LEGAL_COUNSEL\"}"))
            .andExpect(status().isCreated());

        mvc.perform(post("/approvals").with(as("alice", AP_REQUEST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowCode\":\"CONTRACT_VARY\",\"subjectRef\":\"WO-1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.currentRoleCode").value("LEGAL_COUNSEL"))
            .andExpect(jsonPath("$.totalSteps").value(1));
    }

    @Test
    void aWorkflowWithNoStepsCannotApproveAnything() throws Exception {
        mvc.perform(post("/workflows").with(as("admin", WF_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"EMPTY_WF\",\"name\":\"Empty\",\"subjectType\":\"NOTHING\"}"))
            .andExpect(status().isCreated());

        mvc.perform(post("/approvals").with(as("alice", AP_REQUEST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowCode\":\"EMPTY_WF\",\"subjectRef\":\"X-1\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void aDeactivatedWorkflowAcceptsNoNewRequests() throws Exception {
        mvc.perform(post("/workflows/SUPPLIER_DEBAR/deactivate").with(as("admin", WF_MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mvc.perform(post("/approvals").with(as("alice", AP_REQUEST))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workflowCode\":\"SUPPLIER_DEBAR\",\"subjectRef\":\"SUP-1\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void configuringWorkflowsRequiresTheManagePrivilege() throws Exception {
        mvc.perform(post("/workflows").with(as("viewer", WF_VIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"NOPE\",\"name\":\"No\",\"subjectType\":\"X\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownRequestReturns404() throws Exception {
        mvc.perform(get("/approvals/999999").with(as("anyone", AP_VIEW)))
            .andExpect(status().isNotFound());
    }
}
