package com.creatoros.notification.repository;

import com.creatoros.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	List<Notification> findTop100ByOrderByCreatedAtDesc();

	List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
}
