package com.procurementsaas.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Procurement SaaS — Service Template API")
            .version("0.1.0")
            .description("Reference service archetype. Replace title/description per service."));
    }
}
