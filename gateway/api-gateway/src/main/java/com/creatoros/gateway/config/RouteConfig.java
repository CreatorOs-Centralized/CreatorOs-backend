package com.creatoros.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${services.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${services.profile-service.url:http://localhost:8082}")
    private String profileServiceUrl;

    @Value("${services.content-service.url:http://localhost:8083}")
    private String contentServiceUrl;

    @Value("${services.asset-service.url:http://localhost:8084}")
    private String assetServiceUrl;

    @Value("${services.publishing-service.url:http://localhost:8085}")
    private String publishingServiceUrl;

    @Value("${services.scheduler-service.url:http://localhost:8086}")
    private String schedulerServiceUrl;

    @Value("${services.analytics-service.url:http://localhost:8087}")
    private String analyticsServiceUrl;

    @Value("${services.notification-service.url:http://localhost:8088}")
    private String notificationServiceUrl;

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                // Auth Service - Public routes (no JWT required)
                // StripPrefix removes /auth from path before forwarding
                .route("auth-service-public", r -> r
                        .path("/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(authServiceUrl))

                // Profile Service
                .route("profile-service", r -> r
                        .path("/profiles/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(profileServiceUrl))

                // Content Service
                .route("content-service", r -> r
                        .path("/contents/**", "/content/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(contentServiceUrl))

                // Asset Service
                .route("asset-service", r -> r
                        .path("/assets/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(assetServiceUrl))

                // Publishing Service - OAuth routes (public)
                .route("publishing-service-oauth", r -> r
                        .path("/oauth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(publishingServiceUrl))

                // Publishing Service - Protected routes
                .route("publishing-service", r -> r
                        .path("/publishing/**", "/connected-accounts/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(publishingServiceUrl))

                // Scheduler Service
                .route("scheduler-service", r -> r
                        .path("/scheduler/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(schedulerServiceUrl))

                // Analytics Service
                .route("analytics-service", r -> r
                        .path("/analytics/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(analyticsServiceUrl))

                // Notification Service
                .route("notification-service", r -> r
                        .path("/notifications/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(notificationServiceUrl))

                .build();
    }
}
