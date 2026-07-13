package com.procurementsaas.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge service for the Procurement SaaS platform.
 *
 * <p>Authenticates requests against Keycloak (JWT), resolves the tenant, applies rate
 * limiting, and routes traffic to the appropriate microservice.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
