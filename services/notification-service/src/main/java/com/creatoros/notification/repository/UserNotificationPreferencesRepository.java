package com.creatoros.notification.repository;

import com.creatoros.notification.model.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, UUID> {
	Optional<UserNotificationPreferences> findByUserId(UUID userId);
}
