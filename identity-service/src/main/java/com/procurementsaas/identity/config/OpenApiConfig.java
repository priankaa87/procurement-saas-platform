package com.procurementsaas.identity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Identity & Access Service API")
            .version("0.1.0")
            .description("Users, roles, and feature-level RBAC for the Procurement SaaS platform."));
    }
}
