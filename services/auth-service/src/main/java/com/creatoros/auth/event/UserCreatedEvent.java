package com.creatoros.auth.event;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * User lifecycle event emitted by auth-service.
 *
 * <p>Contract rules:
 * <ul>
 *   <li>Minimal and immutable: contains only identity keys and authorization roles.</li>
 *   <li>Versioned: consumers must branch by {@code eventVersion}.</li>
 *   <li>No business data: downstream services must not treat this as a profile payload.</li>
 * </ul>
 */
public record UserCreatedEvent(
                int eventVersion,
        UUID eventId,
        Instant occurredAt,
        String userId,
        String username,
        Set<String> roles
) {

        public UserCreatedEvent {
                if (eventVersion <= 0) {
                        throw new IllegalArgumentException("eventVersion must be > 0");
                }
                Objects.requireNonNull(eventId, "eventId must not be null");
                Objects.requireNonNull(occurredAt, "occurredAt must not be null");
                Objects.requireNonNull(userId, "userId must not be null");

                if (username != null && username.isBlank()) {
                        username = null;
                }
                roles = (roles == null) ? Set.of() : Set.copyOf(roles);
        }
}
