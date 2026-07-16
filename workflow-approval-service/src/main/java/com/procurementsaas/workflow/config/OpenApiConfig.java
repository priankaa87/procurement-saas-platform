package com.procurementsaas.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workflowOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Workflow & Approval Service API")
            .version("0.1.0")
            .description("Configurable approval workflows, approval requests, "
                + "separation of duties, and delegation of authority."));
    }
}
