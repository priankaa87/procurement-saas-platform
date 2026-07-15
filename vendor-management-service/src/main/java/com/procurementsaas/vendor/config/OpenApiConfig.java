package com.procurementsaas.vendor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vendorOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Vendor Management Service API")
            .version("0.1.0")
            .description("Supplier profiles, contacts, documents, and the debarment process."));
    }
}
