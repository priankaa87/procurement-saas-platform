package com.procurementsaas.contract.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI awardContractOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Award & Contract Service API")
            .version("0.1.0")
            .description("Notice of award and acceptance, work orders, delivery schedules, "
                + "and goods receipt."));
    }
}
