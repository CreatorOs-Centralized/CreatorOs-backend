package com.creatoros.notification.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationEventDto(
        UUID userId,
        String email,
        String eventType,
        Map<String, Object> metadata,
        OffsetDateTime scheduledAt
) {
}
