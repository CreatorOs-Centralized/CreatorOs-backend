package com.creatoros.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.creatoros.auth.dto.UserDto;
import com.creatoros.auth.event.UserDeletedEvent;
import com.creatoros.auth.model.Role;
import com.creatoros.auth.model.User;
import com.creatoros.auth.repository.UserRepository;
import com.creatoros.auth.security.AuthenticatedUser;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    private final String topicUserDeleted;

    public UserService(
            UserRepository userRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${creatoros.kafka.topics.user-deleted:auth.user.deleted}") String topicUserDeleted
    ) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topicUserDeleted = topicUserDeleted;
    }

    @Transactional(readOnly = true)
    public UserDto me(AuthenticatedUser principal) {
        UUID id;
        try {
            id = UUID.fromString(principal.userId());
        } catch (IllegalArgumentException ex) {
            return new UserDto(principal.userId(), principal.username(), principal.email(), safeRoles(principal.roles()));
        }

        return userRepository.findWithRolesById(id)
                .map(UserService::toDto)
                .orElseGet(() -> new UserDto(principal.userId(), principal.username(), principal.email(), safeRoles(principal.roles())));
    }

    @Transactional(readOnly = true)
    public Set<String> getUserRoles(String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return Set.of();
        }

        return userRepository.findWithRolesById(id)
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return;
        }

        boolean existed = userRepository.existsById(id);
        if (!existed) {
            return;
        }
        userRepository.deleteById(id);
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
        return new UserDto(user.getId().toString(), null, user.getEmail(), roles);
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
