package com.creatoros.notification.service;

import com.creatoros.notification.dto.NotificationEventDto;
import com.creatoros.notification.model.Notification;
import com.creatoros.notification.model.NotificationChannel;
import com.creatoros.notification.model.NotificationLog;
import com.creatoros.notification.model.NotificationLogLevel;
import com.creatoros.notification.model.NotificationQueueItem;
import com.creatoros.notification.model.NotificationStatus;
import com.creatoros.notification.model.NotificationTopic;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        NotificationTopic notificationTopic = NotificationTopic.fromValue(topic);
        if (notificationTopic == null) {
            log.warn("kafka_message_skipped topic={} reason=unsupported_topic", topic);
            return;
        }

        NotificationEventDto dto;
        try {
            dto = parseEvent(notificationTopic, rawMessage);
        } catch (IllegalArgumentException ex) {
            log.warn("kafka_message_skipped topic={} reason={}", topic, ex.getMessage());
            return;
        }

        Map<String, Object> receivedDetails = new HashMap<>();
        receivedDetails.put("topic", topic);
        receivedDetails.put("event_type", dto.eventType());
        receivedDetails.put("metadata", dto.metadata());
        receivedDetails.put("email", dto.email());

        String notificationType = dto.eventType() == null || dto.eventType().isBlank() ? notificationTopic.value() : dto.eventType();
        String title = buildTitle(notificationTopic, dto);
        String message = buildMessage(notificationTopic, dto);

        NotificationChannel channel = NotificationChannel.EMAIL;
        Notification notification = new Notification(dto.userId(), notificationType, title, message, channel, NotificationStatus.PENDING);
        notification.setScheduledAt(dto.scheduledAt());
        Notification saved = notificationRepository.save(notification);

        logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "event_received", receivedDetails));

        if (!isPreferenceAllowed(notificationTopic, dto.userId(), dto)) {
            markNotificationFailed(saved, "notification_suppressed_by_preferences", Map.of(
                    "topic", topic,
                    "user_id", dto.userId(),
                    "reason", "preference_disabled"
            ));
            return;
        }

        if (dto.email() == null || dto.email().isBlank()) {
            markNotificationFailed(saved, "notification_cannot_send_email", Map.of(
                    "topic", topic,
                    "reason", "missing_email"
            ));
            return;
        }

        Map<String, Object> recipientDetails = new HashMap<>();
        recipientDetails.put("email", dto.email());
        logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "recipient_email", recipientDetails));

        NotificationQueueItem queueItem = new NotificationQueueItem(saved.getId(), QueueProvider.MAILERSEND);
        if (dto.scheduledAt() != null && dto.scheduledAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            queueItem.setNextRetryAt(dto.scheduledAt());
            queueRepository.save(queueItem);
            logRepository.save(new NotificationLog(saved.getId(), NotificationLogLevel.INFO, "notification_scheduled", Map.of(
                    "scheduled_at", dto.scheduledAt(),
                    "queue_item_id", queueItem.getId()
            )));
            return;
        }

        NotificationQueueItem persistedQueueItem = queueRepository.save(queueItem);
        processorService.processQueueItem(persistedQueueItem.getId(), dto.email());
    }

    private NotificationEventDto parseEvent(NotificationTopic topic, String rawMessage) {
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
        OffsetDateTime scheduledAt = parseScheduledAt(root, metadata);
        if (eventType == null || eventType.isBlank()) {
            eventType = topic.value();
        }

        return new NotificationEventDto(userId, email, eventType, metadata, scheduledAt);
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
        map.remove("scheduled_at");
        map.remove("scheduledAt");
        return map;
    }

    private OffsetDateTime parseScheduledAt(JsonNode root, Map<String, Object> metadata) {
        String rootScheduledAt = text(root, "scheduled_at");
        if (rootScheduledAt == null) {
            rootScheduledAt = text(root, "scheduledAt");
        }

        if (rootScheduledAt != null && !rootScheduledAt.isBlank()) {
            return parseIsoDateTime(rootScheduledAt);
        }

        Object metadataScheduledAt = metadata.get("scheduled_at");
        if (metadataScheduledAt == null) {
            metadataScheduledAt = metadata.get("scheduledAt");
        }

        if (metadataScheduledAt instanceof String scheduledAtString && !scheduledAtString.isBlank()) {
            return parseIsoDateTime(scheduledAtString);
        }

        return null;
    }

    private OffsetDateTime parseIsoDateTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isPreferenceAllowed(NotificationTopic topic, UUID userId, NotificationEventDto dto) {
        return preferencesRepository.findByUserId(userId)
                .map(prefs -> {
                    if (!prefs.isEmailEnabled()) {
                        return false;
                    }
                    return switch (topic) {
                        case PUBLISH_SUCCEEDED -> prefs.isPublishSuccessAlerts();
                        case PUBLISH_FAILED, PUBLISH_RETRY_REQUESTED -> prefs.isPublishFailureAlerts();
                        case NOTIFICATION_SEND_REQUESTED -> !isScheduleReminder(dto) || prefs.isScheduleReminders();
                        case PUBLISH_STARTED -> true;
                    };
                })
                .orElse(true);
    }

    private boolean isScheduleReminder(NotificationEventDto dto) {
        if (dto.eventType() != null) {
            String type = dto.eventType().toLowerCase();
            if (type.contains("schedule") || type.contains("reminder")) {
                return true;
            }
        }

        Map<String, Object> metadata = Optional.ofNullable(dto.metadata()).orElse(Map.of());
        Object category = metadata.get("category");
        if (category instanceof String categoryStr) {
            String normalized = categoryStr.toLowerCase();
            if (normalized.contains("schedule") || normalized.contains("reminder")) {
                return true;
            }
        }

        return false;
    }

    private void markNotificationFailed(Notification notification, String message, Map<String, Object> details) {
        notification.setStatus(NotificationStatus.FAILED);
        logRepository.save(new NotificationLog(notification.getId(), NotificationLogLevel.WARN, message, details));
    }

    private static String buildTitle(NotificationTopic topic, NotificationEventDto dto) {
        return switch (topic) {
            case PUBLISH_STARTED -> "Publish started";
            case PUBLISH_SUCCEEDED -> "Publish succeeded";
            case PUBLISH_FAILED -> "Publish failed";
            case PUBLISH_RETRY_REQUESTED -> "Publish retry requested";
            case NOTIFICATION_SEND_REQUESTED -> {
                Map<String, Object> meta = Optional.ofNullable(dto.metadata()).orElse(Map.of());
                Object title = meta.get("title");
                yield title == null ? "Notification requested" : String.valueOf(title);
            }
        };
    }

    private static String buildMessage(NotificationTopic topic, NotificationEventDto dto) {
        Map<String, Object> meta = Optional.ofNullable(dto.metadata()).orElse(Map.of());
        if (NotificationTopic.PUBLISH_STARTED.equals(topic)) {
            Object platform = meta.get("platform");
            if (platform != null) {
                return "Publishing has started for " + platform + ".";
            }
            return "Publishing has started.";
        }
        if (NotificationTopic.PUBLISH_SUCCEEDED.equals(topic)) {
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
        if (NotificationTopic.PUBLISH_FAILED.equals(topic)) {
            Object platform = meta.get("platform");
            Object error = meta.get("error");
            if (platform != null && error != null) {
                return "Publishing to " + platform + " failed. Error: " + error;
            }
            return "Publishing failed.";
        }
        if (NotificationTopic.PUBLISH_RETRY_REQUESTED.equals(topic)) {
            return "A publish retry was requested.";
        }
        if (NotificationTopic.NOTIFICATION_SEND_REQUESTED.equals(topic)) {
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
