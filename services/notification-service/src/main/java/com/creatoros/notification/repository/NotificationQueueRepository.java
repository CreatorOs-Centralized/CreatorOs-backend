package com.creatoros.notification.repository;

import com.creatoros.notification.model.NotificationQueueItem;
import com.creatoros.notification.model.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationQueueRepository extends JpaRepository<NotificationQueueItem, UUID> {

    List<NotificationQueueItem> findTop100ByStatusInAndNextRetryAtBeforeOrderByNextRetryAtAsc(Collection<QueueStatus> statuses, OffsetDateTime before);
}
