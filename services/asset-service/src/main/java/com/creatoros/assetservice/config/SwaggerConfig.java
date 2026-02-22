package com.creatoros.assetservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI assetServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Asset Service API")
                        .description("API for managing assets, folders, and uploads.")
                        .version("v1.0"));
    }
}
