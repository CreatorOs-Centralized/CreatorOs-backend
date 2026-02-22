package com.creatoros.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security Configuration for API Gateway
 * 
 * The gateway uses JwtAuthenticationFilter as a global filter for JWT validation.
 * This config disables default security to let the GlobalFilter handle all authentication.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            // Match all paths and permit all - let JwtAuthenticationFilter handle auth
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            );

        return http.build();
    }
}
