package com.procurementsaas.tenantbilling.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tenantBillingOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Tenant & Billing Service API")
            .version("0.1.0")
            .description("SaaS control plane: tenant onboarding and provisioning, plans, "
                + "entitlements, usage metering, invoicing."));
    }
}
