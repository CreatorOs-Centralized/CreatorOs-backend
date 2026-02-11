package com.creatoros.notification.repository;

import com.creatoros.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

	Optional<NotificationLog> findTopByNotificationIdAndMessageOrderByCreatedAtDesc(UUID notificationId, String message);
}
