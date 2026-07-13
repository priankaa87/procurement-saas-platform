package com.procurementsaas.masterdata;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test against a real PostgreSQL (Testcontainers): validates Flyway migration,
 * Hibernate schema validation, feature-level security, caching wiring, and the item
 * creation flow that resolves seeded category + unit references.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MasterDataServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    private static SimpleGrantedAuthority view() {
        return new SimpleGrantedAuthority("FEATURE_MASTERDATA_VIEW");
    }

    private static SimpleGrantedAuthority manage() {
        return new SimpleGrantedAuthority("FEATURE_MASTERDATA_MANAGE");
    }

    @Test
    void contextLoadsAndSchemaValidates() {
        // Context start implies Flyway applied V1 and Hibernate validated the mappings.
    }

    @Test
    void readsRequireAuthentication() throws Exception {
        mvc.perform(get("/units")).andExpect(status().isUnauthorized());
    }

    @Test
    void seededUnitsAreListed() throws Exception {
        mvc.perform(get("/units").with(jwt().authorities(view())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void seededCurrenciesAreListed() throws Exception {
        mvc.perform(get("/currencies").with(jwt().authorities(view())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void createRequiresManageAuthority() throws Exception {
        String body = "{\"code\":\"TON\",\"name\":\"Metric Ton\",\"symbol\":\"t\"}";
        mvc.perform(post("/units").with(jwt().authorities(view()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void createItemResolvesSeededCategoryAndUnit() throws Exception {
        String body = """
            {"code":"LAPTOP-01","name":"Business Laptop","description":"14 inch",
             "categoryCode":"IT-HARDWARE","unitCode":"PCS"}
            """;
        mvc.perform(post("/items").with(jwt().authorities(manage()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("LAPTOP-01"))
            .andExpect(jsonPath("$.categoryCode").value("IT-HARDWARE"))
            .andExpect(jsonPath("$.unitCode").value("PCS"));
    }

    @Test
    void createItemWithUnknownCategoryReturns404() throws Exception {
        String body = """
            {"code":"X-1","name":"Bad","description":null,
             "categoryCode":"NOPE","unitCode":"PCS"}
            """;
        mvc.perform(post("/items").with(jwt().authorities(manage()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void listCitiesForSeededCountry() throws Exception {
        // No cities seeded, but the country exists; endpoint returns an empty list.
        mvc.perform(get("/countries/US/cities").with(jwt().authorities(view())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
