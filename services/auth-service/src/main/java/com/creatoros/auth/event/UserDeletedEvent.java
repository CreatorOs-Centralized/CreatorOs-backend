package com.creatoros.auth.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * User lifecycle event emitted by auth-service when a user is deleted from auth-service owned storage.
 */
public record UserDeletedEvent(
                int eventVersion,
        UUID eventId,
        Instant occurredAt,
        String userId
) {

        public UserDeletedEvent {
                if (eventVersion <= 0) {
                        throw new IllegalArgumentException("eventVersion must be > 0");
                }
                Objects.requireNonNull(eventId, "eventId must not be null");
                Objects.requireNonNull(occurredAt, "occurredAt must not be null");
                Objects.requireNonNull(userId, "userId must not be null");
        }
}
