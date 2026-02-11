package com.creatoros.notification.service;

import com.creatoros.notification.dto.NotificationEventDto;
import com.creatoros.notification.model.Notification;
import com.creatoros.notification.model.NotificationChannel;
import com.creatoros.notification.model.NotificationLog;
import com.creatoros.notification.model.NotificationLogLevel;
import com.creatoros.notification.model.NotificationQueueItem;
import com.creatoros.notification.model.NotificationStatus;
import com.creatoros.notification.model.QueueProvider;
import com.creatoros.notification.repository.NotificationLogRepository;
import com.creatoros.notification.repository.NotificationQueueRepository;
import com.creatoros.notification.repository.NotificationRepository;
import com.creatoros.notification.repository.UserNotificationPreferencesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationConsumerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumerService.class);

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final NotificationQueueRepository queueRepository;
    private final NotificationLogRepository logRepository;
    private final UserNotificationPreferencesRepository preferencesRepository;
    private final NotificationProcessorService processorService;

    public NotificationConsumerService(
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository,
            NotificationQueueRepository queueRepository,
            NotificationLogRepository logRepository,
            UserNotificationPreferencesRepository preferencesRepository,
            NotificationProcessorService processorService
    ) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.queueRepository = queueRepository;
        this.logRepository = logRepository;
        this.preferencesRepository = preferencesRepository;
        this.processorService = processorService;
    }

    @Transactional
    public void consume(String topic, String rawMessage) {
        NotificationEventDto dto;
        try {
            dto = parseEvent(topic, rawMessage);
        } catch (IllegalArgumentException ex) {
            log.warn("kafka_message_skipped topic={} reason={}", topic, ex.getMessage());
            return;
        }

        Map<String, Object> receivedDetails = new HashMap<>();
        receivedDetails.put("topic", topic);
        receivedDetails.put("event_type", dto.eventType());
        receivedDetails.put("metadata", dto.metadata());
        receivedDetails.put("email", dto.email());

        String notificationType = dto.eventType() == null || dto.eventType().isBlank() ? topic : dto.eventType();

        String title = switch (topic) {
            case "publish.succeeded" -> "Publish succeeded";
            case "publish.failed" -> "Publish failed";
            case "publish.retry.requested" -> "Publish retry requested";
            case "notification.send.requested" -> "Notification requested";
            default -> "Notification";
        };

        String message = buildMessage(topic, dto);

        NotificationChannel channel = NotificationChannel.EMAIL;
        Notification notification = new Notification(dto.userId(), notificationType, title, message, channel, NotificationStatus.PENDING);
        Notification saved = notificationRepository.save(notification);

        logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "event_received", receivedDetails));

        if (dto.email() == null || dto.email().isBlank()) {
            saved.setStatus(NotificationStatus.FAILED);
            Map<String, Object> details = new HashMap<>();
            details.put("topic", topic);
            details.put("reason", "missing_email");
            logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.ERROR, "notification_cannot_send_email", details));
            return;
        }

        Map<String, Object> recipientDetails = new HashMap<>();
        recipientDetails.put("email", dto.email());
        logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "recipient_email", recipientDetails));

        // Preferences are persisted here for future use; for now we only enforce email_enabled when present.
        preferencesRepository.findById(dto.userId()).ifPresent(prefs -> {
            if (!prefs.isEmailEnabled()) {
                saved.setStatus(NotificationStatus.FAILED);
                Map<String, Object> details = new HashMap<>();
                details.put("topic", topic);
                details.put("reason", "email_disabled");
                logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "notification_email_disabled", details));
            }
        });

        if (saved.getStatus() == NotificationStatus.FAILED) {
            return;
        }

        NotificationQueueItem queueItem = new NotificationQueueItem(saved.getId(), QueueProvider.MAILERSEND);
        NotificationQueueItem persistedQueueItem = queueRepository.save(queueItem);

        processorService.processQueueItem(persistedQueueItem.getId(), dto.email());
    }

    private NotificationEventDto parseEvent(String topic, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("empty_message");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawMessage);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid_json");
        }

        String userIdStr = text(root, "user_id");
        if (userIdStr == null) {
            userIdStr = text(root, "userId");
        }

        if (userIdStr == null || userIdStr.isBlank()) {
            throw new IllegalArgumentException("missing_user_id");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid_user_id");
        }

        String email = text(root, "email");
        String eventType = text(root, "event_type");
        if (eventType == null) {
            eventType = text(root, "eventType");
        }

        Map<String, Object> metadata = extractMetadata(root);
        if (eventType == null || eventType.isBlank()) {
            eventType = topic;
        }

        return new NotificationEventDto(userId, email, eventType, metadata);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Map<String, Object> extractMetadata(JsonNode root) {
        JsonNode metadataNode = root.get("metadata");
        if (metadataNode != null && metadataNode.isObject()) {
            return objectMapper.convertValue(metadataNode, new TypeReference<>() {});
        }

        // Backward-compat: treat the entire payload as metadata when a dedicated metadata object is not present.
        Map<String, Object> map = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
        map.remove("user_id");
        map.remove("userId");
        map.remove("email");
        map.remove("event_type");
        map.remove("eventType");
        return map;
    }

    private static String buildMessage(String topic, NotificationEventDto dto) {
        Map<String, Object> meta = Optional.ofNullable(dto.metadata()).orElse(Map.of());
        if ("publish.succeeded".equals(topic)) {
            Object platform = meta.get("platform");
            Object permalink = meta.get("permalink");
            if (platform != null && permalink != null) {
                return "Your post was published successfully to " + platform + ". Link: " + permalink;
            }
            if (platform != null) {
                return "Your post was published successfully to " + platform + ".";
            }
            return "Your post was published successfully.";
        }
        if ("publish.failed".equals(topic)) {
            Object platform = meta.get("platform");
            Object error = meta.get("error");
            if (platform != null && error != null) {
                return "Publishing to " + platform + " failed. Error: " + error;
            }
            return "Publishing failed.";
        }
        if ("publish.retry.requested".equals(topic)) {
            return "A publish retry was requested.";
        }
        if ("notification.send.requested".equals(topic)) {
            Object title = meta.get("title");
            Object message = meta.get("message");
            if (title != null && message != null) {
                return String.valueOf(title) + ": " + String.valueOf(message);
            }
            if (message != null) {
                return String.valueOf(message);
            }
            return "A notification was requested.";
        }
        return "Event received: " + (dto.eventType() == null ? topic : dto.eventType());
    }
}
