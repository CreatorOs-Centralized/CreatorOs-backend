package com.creatoros.publishing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI publishingServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CreatorOS Publishing Service API")
                        .description("API for publishing content and managing publish jobs")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("CreatorOS Team")
                                .email("support@creatoros.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Local Development"),
                        new Server().url("https://api.creatoros.com/publishing").description("Production")
                ));
    }
}
