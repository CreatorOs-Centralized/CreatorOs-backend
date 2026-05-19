package com.creatoros.publishing.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// @Configuration
// CORS is handled by API Gateway - this configuration is disabled to prevent duplicate headers
public class CorsConfig {

    // @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${creatoros.cors.allowed-origin-patterns:https://creatoros.adharbattulwar.com,https://creatoros-api.adharbattulwar.com,https://*.adharbattulwar.com,http://localhost:*,https://*.ngrok-free.dev,https://creator-os-frontend-final.vercel.app}") String allowedOriginPatterns
    ) {
        List<String> originPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(originPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}