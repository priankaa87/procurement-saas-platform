package com.procurementsaas.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reference Spring Boot 3.4 / Java 21 service archetype.
 *
 * <p>Copy this module to create a business service (identity, vendor, tender, ...).
 * It wires the cross-cutting concerns every service shares: OAuth2 resource-server
 * security, schema-per-tenant multi-tenancy, OpenAPI, Flyway, and Actuator.
 */
@SpringBootApplication
public class ServiceTemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceTemplateApplication.class, args);
    }
}
