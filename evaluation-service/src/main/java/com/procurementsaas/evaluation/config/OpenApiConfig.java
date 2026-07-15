package com.procurementsaas.evaluation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evaluationOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Evaluation Service API")
            .version("0.1.0")
            .description("Technical and financial bid evaluation, ranking, comparative statement."));
    }
}
