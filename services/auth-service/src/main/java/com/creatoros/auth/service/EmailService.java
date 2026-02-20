package com.creatoros.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.creatoros.auth.config.AuthProperties;
import com.creatoros.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;
    private final String ingestPath;
    private final String topic;

    public EmailService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AuthProperties authProperties,
            @Value("${creatoros.notification-service.base-url:http://notification-service:8090}") String notificationServiceBaseUrl,
            @Value("${creatoros.notification-service.ingest-path:/api/v1/internal/notifications/ingest}") String ingestPath,
            @Value("${creatoros.notification-service.topic:notification.send.requested}") String topic
    ) {
        this.restClient = restClientBuilder.baseUrl(notificationServiceBaseUrl).build();
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
        this.ingestPath = ingestPath;
        this.topic = topic;
    }

    public void sendEmailVerification(User user, String rawToken) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String base = authProperties.getFrontendBaseUrl() == null ? "" : authProperties.getFrontendBaseUrl().trim();
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String url = base + "/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        String title = "Verify your CreatorOS email";
        String message = "Welcome to CreatorOS!\n\nPlease verify your email by clicking the link below:\n" + url
            + "\n\nIf you didn't create this account, you can ignore this email.";

        Map<String, Object> payload = Map.of(
            "user_id", String.valueOf(user.getId()),
            "email", user.getEmail(),
            "event_type", "auth.verify_email",
            "metadata", Map.of(
                "title", title,
                "message", message
            )
        );

        // Best-effort: do not block registration success on notification provider issues.
        try {
            String rawJson = objectMapper.writeValueAsString(payload);
            restClient.post()
                .uri(uriBuilder -> uriBuilder.path(ingestPath).queryParam("topic", topic).build())
                .contentType(MediaType.TEXT_PLAIN)
                .body(rawJson)
                .retrieve()
                .toBodilessEntity();

            log.info("notification_requested type=email_verification to={}", user.getEmail());
        } catch (Exception ex) {
            log.warn("notification_request_failed type=email_verification to={}", user.getEmail(), ex);
        }
    }
}
