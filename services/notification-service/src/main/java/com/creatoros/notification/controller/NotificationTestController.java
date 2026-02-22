package com.creatoros.notification.controller;

import com.creatoros.notification.model.Notification;
import com.creatoros.notification.repository.NotificationRepository;
import com.creatoros.notification.service.NotificationConsumerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class NotificationTestController {

    private final NotificationConsumerService consumerService;
    private final NotificationRepository notificationRepository;

    public NotificationTestController(
            NotificationConsumerService consumerService,
            NotificationRepository notificationRepository
    ) {
        this.consumerService = consumerService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/api/v1/internal/notifications/ingest")
    public ResponseEntity<Map<String, Object>> ingest(
            @RequestParam String topic,
            @RequestBody String payload
    ) {
        consumerService.consume(topic, payload);
        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "topic", topic
        ));
    }

    @GetMapping("/api/v1/internal/notifications")
    public ResponseEntity<List<Notification>> list(
            @RequestParam(required = false) UUID userId
    ) {
        if (userId == null) {
            return ResponseEntity.ok(notificationRepository.findTop100ByOrderByCreatedAtDesc());
        }
        return ResponseEntity.ok(notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId));
    }
}