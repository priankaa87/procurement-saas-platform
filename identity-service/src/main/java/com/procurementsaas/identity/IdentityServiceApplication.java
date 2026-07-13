package com.procurementsaas.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Identity &amp; Access Service.
 *
 * <p>Owns users, roles, and the fine-grained, feature-level RBAC model. Authentication
 * itself is delegated to Keycloak (OIDC); this service manages the authorization data and
 * exposes it, and validates the resulting JWTs as an OAuth2 resource server.
 */
@SpringBootApplication
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
