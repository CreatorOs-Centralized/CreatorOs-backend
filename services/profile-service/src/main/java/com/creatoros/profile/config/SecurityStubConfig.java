package com.creatoros.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Stub Configuration
 * 
 * Temporary security configuration for development.
 * TODO: Replace with proper security implementation with JWT validation
 * and integration with auth-service.
 */
@Configuration
@EnableWebSecurity
public class SecurityStubConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**",
                    "/actuator/health/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {}); // Temporary: basic auth for development

        return http.build();
    }
}
