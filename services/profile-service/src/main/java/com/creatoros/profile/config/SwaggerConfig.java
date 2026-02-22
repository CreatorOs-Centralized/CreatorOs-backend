package com.creatoros.profile.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration
 * 
 * Configures API documentation for the Profile Service.
 * Accessible at: /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI profileServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CreatorOS Profile Service API")
                        .description("API for managing creator profiles, bio, and social links")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("CreatorOS Team")
                                .email("support@creatoros.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Development"),
                        new Server().url("https://api.creatoros.com/profile").description("Production")
                ));
    }
}
