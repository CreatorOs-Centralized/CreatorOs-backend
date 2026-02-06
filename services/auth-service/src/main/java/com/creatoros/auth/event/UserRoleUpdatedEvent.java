package com.creatoros.auth.event;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * User authorization event emitted by auth-service when role membership changes.
 *
 * <p>Consumers should treat this as the full current role set for the user (not a delta).
 */
public record UserRoleUpdatedEvent(
                int eventVersion,
        UUID eventId,
        Instant occurredAt,
        String userId,
        Set<String> roles
) {

        public UserRoleUpdatedEvent {
                if (eventVersion <= 0) {
                        throw new IllegalArgumentException("eventVersion must be > 0");
                }
                Objects.requireNonNull(eventId, "eventId must not be null");
                Objects.requireNonNull(occurredAt, "occurredAt must not be null");
                Objects.requireNonNull(userId, "userId must not be null");
                roles = (roles == null) ? Set.of() : Set.copyOf(roles);
        }
}
