package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.creatoros.auth.dto.UserDto;
import com.creatoros.auth.event.UserCreatedEvent;
import com.creatoros.auth.event.UserDeletedEvent;
import com.creatoros.auth.event.UserRoleUpdatedEvent;
import com.creatoros.auth.model.Role;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.UserRepository;
import com.creatoros.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final KeycloakSyncService keycloakSyncService;
    private final SessionService sessionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    private final String topicUserCreated;
    private final String topicUserRoleUpdated;
    private final String topicUserDeleted;

    public UserService(
            UserRepository userRepository,
            RoleService roleService,
            KeycloakSyncService keycloakSyncService,
            SessionService sessionService,
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${creatoros.kafka.topics.user-created:auth.user.created}") String topicUserCreated,
            @Value("${creatoros.kafka.topics.user-role-updated:auth.user.role-updated}") String topicUserRoleUpdated,
            @Value("${creatoros.kafka.topics.user-deleted:auth.user.deleted}") String topicUserDeleted
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.keycloakSyncService = keycloakSyncService;
        this.sessionService = sessionService;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topicUserCreated = topicUserCreated;
        this.topicUserRoleUpdated = topicUserRoleUpdated;
        this.topicUserDeleted = topicUserDeleted;
    }

    @Transactional(readOnly = true)
    public UserDto me(AuthenticatedUser principal) {
        return userRepository.findWithRolesById(principal.userId())
                .map(UserService::toDto)
                .orElseGet(() -> new UserDto(principal.userId(), principal.username(), principal.email(), safeRoles(principal.roles())));
    }

    @Transactional(readOnly = true)
    public Set<String> getUserRoles(String userId) {
        return userRepository.findWithRolesById(userId)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new);
    }

    @Transactional
    public UserDto syncCurrentUser(AuthenticatedUser principal, HttpServletRequest request) {
        KeycloakSyncService.KeycloakUserSnapshot snapshot = keycloakSyncService.snapshotFromToken(principal);
        if (snapshot.userId() == null || snapshot.userId().isBlank()) {
            throw new IllegalArgumentException("Missing userId (sub) in JWT");
        }

        Optional<User> existing = userRepository.findWithRolesById(snapshot.userId());
        boolean isNew = existing.isEmpty();
        User user = existing.orElseGet(() -> new User(snapshot.userId()));

        Set<String> previousRoles = user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        keycloakSyncService.applyToUser(user, snapshot);
        Set<Role> roleEntities = roleService.upsertRoles(snapshot.roles());
        user.setRoles(roleEntities);

        User saved = userRepository.save(user);
        sessionService.recordSessionIfMissing(saved, principal, request);

        Set<String> desiredRoles = safeRoles(snapshot.roles());
        Instant now = Instant.now(clock);
        if (isNew) {
            publishBestEffort(topicUserCreated, saved.getId(), new UserCreatedEvent(
                    1,
                UUID.randomUUID(),
                now,
                saved.getId(),
                saved.getUsername(),
                desiredRoles
            ));
        } else if (!previousRoles.equals(desiredRoles)) {
            publishBestEffort(topicUserRoleUpdated, saved.getId(), new UserRoleUpdatedEvent(
                    1,
                UUID.randomUUID(),
                now,
                saved.getId(),
                desiredRoles
            ));
        }

        return toDto(saved);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        boolean existed = userRepository.existsById(userId);
        if (!existed) {
            return;
        }
        userRepository.deleteById(userId);
        publishBestEffort(topicUserDeleted, userId, new UserDeletedEvent(1, UUID.randomUUID(), Instant.now(clock), userId));
    }

    private void publishBestEffort(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event);
        } catch (RuntimeException ex) {
            String eventType = event == null ? "null" : event.getClass().getSimpleName();
            log.warn("kafka_publish_failed topic={} key={} eventType={}", topic, key, eventType, ex);
        }
    }

    private static UserDto toDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), roles);
    }

    private static Set<String> safeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
