package com.procurementsaas.identity;

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
 * End-to-end test against a real PostgreSQL (Testcontainers). Verifies that:
 * <ul>
 *   <li>the context loads, Flyway migrates, and Hibernate {@code ddl-auto=validate} passes
 *       (i.e. entities match the schema);</li>
 *   <li>endpoints enforce feature-level authorities;</li>
 *   <li>the user-creation flow works with the seeded ADMIN role.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IdentityServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    // The JWT decoder is mocked so the context never contacts Keycloak; requests are
    // authenticated with the jwt() post-processor, which sets authorities directly.
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mvc;

    @Test
    void contextLoadsAndSchemaValidates() {
        // If this test runs at all, the context started: Flyway applied V1 and Hibernate
        // validated the mapping against the migrated schema.
    }

    @Test
    void featuresRequireAuthentication() throws Exception {
        mvc.perform(get("/features"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void featuresListedWithFeatureAuthority() throws Exception {
        mvc.perform(get("/features")
                .with(jwt().authorities(new SimpleGrantedAuthority("FEATURE_FEATURE_VIEW"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void forbiddenWithoutRequiredFeature() throws Exception {
        mvc.perform(get("/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("FEATURE_UNRELATED"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void createUserWithSeededAdminRole() throws Exception {
        String body = """
            {"username":"alice","email":"alice@example.com","fullName":"Alice","roleCodes":["ADMIN"]}
            """;
        mvc.perform(post("/users")
                .with(jwt().authorities(new SimpleGrantedAuthority("FEATURE_USER_MANAGE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void meReturnsPrincipalAndTenant() throws Exception {
        mvc.perform(get("/me")
                .with(jwt().jwt(j -> j.subject("u-1").claim("preferred_username", "bob"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("bob"))
            .andExpect(jsonPath("$.tenant").value("public"));
    }
}
