package com.procurementsaas.tenantbilling;

import com.procurementsaas.tenantbilling.provisioning.TenantProvisioner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the SaaS control plane against a real PostgreSQL.
 *
 * <p>The important ones assert that onboarding genuinely creates and migrates a schema in
 * the database — the multi-tenancy story is only real if that actually happens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantBillingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TenantProvisioner provisioner;

    private static final String VIEW = "FEATURE_TENANT_VIEW";
    private static final String MANAGE = "FEATURE_TENANT_MANAGE";
    private static final String BILL_VIEW = "FEATURE_BILLING_VIEW";
    private static final String BILL_MANAGE = "FEATURE_BILLING_MANAGE";

    private static RequestPostProcessor with(String feature) {
        return jwt().authorities(new SimpleGrantedAuthority(feature));
    }

    private void onboard(String key, String plan) throws Exception {
        mvc.perform(post("/tenants").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantKey":"%s","name":"%s Ltd","planCode":"%s","contactEmail":"ops@%s.com"}
                    """.formatted(key, key, plan, key)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.schemaName").value("tenant_" + key));
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied the control-plane migration and Hibernate
        // validated the mappings.
    }

    @Test
    void seededPlansAreAvailable() throws Exception {
        mvc.perform(get("/plans").with(with(VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[?(@.code=='FREE')].maxUsers").value(3));
    }

    @Test
    void tenantAdministrationRequiresAuthentication() throws Exception {
        mvc.perform(get("/tenants")).andExpect(status().isUnauthorized());
    }

    /** The heart of the multi-tenancy design: onboarding must really create a schema. */
    @Test
    void onboardingCreatesAndMigratesARealSchema() throws Exception {
        onboard("acme", "STARTER");

        Integer schemaCount = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.schemata WHERE schema_name = ?",
            Integer.class, "tenant_acme");
        assertThat(schemaCount).isEqualTo(1);

        // The baseline migration ran inside the new schema, not the shared one.
        Integer tableCount = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ?",
            Integer.class, "tenant_acme", "tenant_info");
        assertThat(tableCount).isEqualTo(1);

        // Flyway tracked the migration in the tenant's own schema.
        Integer historyCount = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ?",
            Integer.class, "tenant_acme", "flyway_schema_history");
        assertThat(historyCount).isEqualTo(1);
    }

    @Test
    void eachTenantGetsItsOwnIsolatedSchema() throws Exception {
        onboard("alpha", "FREE");
        onboard("beta", "FREE");

        assertThat(provisioner.schemaExists("tenant_alpha")).isTrue();
        assertThat(provisioner.schemaExists("tenant_beta")).isTrue();

        // Writing in one tenant's schema leaves the other untouched.
        jdbc.execute("INSERT INTO tenant_alpha.tenant_info (tenant_key, schema_version) "
            + "VALUES ('extra', '1.0')");
        Integer alpha = jdbc.queryForObject(
            "SELECT count(*) FROM tenant_alpha.tenant_info", Integer.class);
        Integer beta = jdbc.queryForObject(
            "SELECT count(*) FROM tenant_beta.tenant_info", Integer.class);
        assertThat(alpha).isEqualTo(2);
        assertThat(beta).isEqualTo(1);
    }

    @Test
    void provisioningIsIdempotentSoARetriedOnboardingConverges() {
        provisioner.provision("tenant_retry");
        // A retry after a partial failure must not blow up.
        provisioner.provision("tenant_retry");
        assertThat(provisioner.schemaExists("tenant_retry")).isTrue();
    }

    @Test
    void aHostileSchemaNameNeverReachesTheDatabase() {
        // Belt-and-braces check inside the provisioner itself.
        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> provisioner.provision("public"))
            .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> provisioner.provision("tenant_x\"; DROP SCHEMA public; --"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aReservedTenantKeyIsRejected() throws Exception {
        mvc.perform(post("/tenants").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantKey\":\"public\",\"name\":\"Evil\",\"planCode\":\"FREE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aTenantKeyCannotBeTakenTwice() throws Exception {
        onboard("dupe", "FREE");
        mvc.perform(post("/tenants").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantKey\":\"dupe\",\"name\":\"Other\",\"planCode\":\"FREE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void usageIsCappedByThePlan() throws Exception {
        onboard("capped", "FREE");   // FREE allows 3 users

        mvc.perform(post("/tenants/capped/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements[?(@.metric=='users')].used").value(3))
            .andExpect(jsonPath("$.entitlements[?(@.metric=='users')].remaining").value(0));

        // The fourth user breaches the plan.
        mvc.perform(post("/tenants/capped/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":1}"))
            .andExpect(status().isConflict());
    }

    @Test
    void upgradingThePlanRaisesTheCap() throws Exception {
        onboard("upgrade", "FREE");
        mvc.perform(post("/tenants/upgrade/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":3}"))
            .andExpect(status().isOk());
        mvc.perform(post("/tenants/upgrade/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":1}"))
            .andExpect(status().isConflict());

        mvc.perform(put("/tenants/upgrade/plan").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\":\"STARTER\"}"))
            .andExpect(status().isOk());

        // Same request, now within the STARTER limit of 10.
        mvc.perform(post("/tenants/upgrade/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":1}"))
            .andExpect(status().isOk());
    }

    @Test
    void anUnlimitedPlanNeverBlocksUsage() throws Exception {
        onboard("unlimited", "ENTERPRISE");   // -1 = unlimited
        mvc.perform(post("/tenants/unlimited/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"tenders\",\"quantity\":100000}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements[?(@.metric=='tenders')].unlimited").value(true));
    }

    @Test
    void aSuspendedTenantCannotConsumeAnything() throws Exception {
        onboard("suspended", "STARTER");
        mvc.perform(post("/tenants/suspended/suspend").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mvc.perform(post("/tenants/suspended/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":1}"))
            .andExpect(status().isConflict());

        mvc.perform(post("/tenants/suspended/reactivate").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/tenants/suspended/usage").with(with(MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metric\":\"users\",\"quantity\":1}"))
            .andExpect(status().isOk());
    }

    @Test
    void cancellingATenantKeepsItsDataIntact() throws Exception {
        onboard("leaving", "STARTER");
        mvc.perform(post("/tenants/leaving/cancel").with(with(MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cancellation is a billing decision, not a data-destruction one.
        assertThat(provisioner.schemaExists("tenant_leaving")).isTrue();
    }

    @Test
    void invoicingChargesThePlanPriceForTheMonth() throws Exception {
        onboard("billed", "STARTER");   // 99.00 USD

        mvc.perform(post("/tenants/billed/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periodStart\":\"2027-03-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(99.00))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.periodEnd").value("2027-03-31"))
            .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void aPeriodIsNeverInvoicedTwice() throws Exception {
        onboard("norepeat", "STARTER");
        mvc.perform(post("/tenants/norepeat/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periodStart\":\"2027-04-01\"}"))
            .andExpect(status().isCreated());

        // A retried billing run must not charge the customer again.
        mvc.perform(post("/tenants/norepeat/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periodStart\":\"2027-04-15\"}"))   // same month
            .andExpect(status().isBadRequest());
    }

    @Test
    void aPaidInvoiceCannotBeVoided() throws Exception {
        onboard("paid", "STARTER");
        String json = mvc.perform(post("/tenants/paid/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periodStart\":\"2027-05-01\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long invoiceId = com.jayway.jsonpath.JsonPath.parse(json).read("$.id", Integer.class);

        mvc.perform(post("/invoices/" + invoiceId + "/pay").with(with(BILL_MANAGE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));

        mvc.perform(post("/invoices/" + invoiceId + "/void").with(with(BILL_MANAGE)))
            .andExpect(status().isConflict());
    }

    @Test
    void invoiceHistoryIsListedNewestFirst() throws Exception {
        onboard("history", "STARTER");
        mvc.perform(post("/tenants/history/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"periodStart\":\"2027-01-01\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/tenants/history/invoices").with(with(BILL_MANAGE))
                .contentType(MediaType.APPLICATION_JSON).content("{\"periodStart\":\"2027-02-01\"}"))
            .andExpect(status().isCreated());

        mvc.perform(get("/tenants/history/invoices").with(with(BILL_VIEW)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].periodStart").value("2027-02-01"));
    }

    @Test
    void tenantAdministrationRequiresTheManagePrivilege() throws Exception {
        mvc.perform(post("/tenants").with(with(VIEW))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantKey\":\"nope\",\"name\":\"No\",\"planCode\":\"FREE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownTenantReturns404() throws Exception {
        mvc.perform(get("/tenants/ghost").with(with(VIEW)))
            .andExpect(status().isNotFound());
    }
}
