package com.procurementsaas.masterdata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI masterDataOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Master Data Service API")
            .version("0.1.0")
            .description("Shared reference data: units, currencies, item categories/items, geography."));
    }
}
