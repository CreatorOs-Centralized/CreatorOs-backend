package com.creatoros.notification.service;

import com.creatoros.notification.model.Notification;
import com.creatoros.notification.model.NotificationLog;
import com.creatoros.notification.model.NotificationLogLevel;
import com.creatoros.notification.model.NotificationQueueItem;
import com.creatoros.notification.model.QueueStatus;
import com.creatoros.notification.repository.NotificationLogRepository;
import com.creatoros.notification.repository.NotificationQueueRepository;
import com.creatoros.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QueueRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueRetryScheduler.class);

    private final NotificationQueueRepository queueRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationProcessorService processorService;

    public QueueRetryScheduler(
            NotificationQueueRepository queueRepository,
            NotificationRepository notificationRepository,
            NotificationLogRepository logRepository,
            NotificationProcessorService processorService
    ) {
        this.queueRepository = queueRepository;
        this.notificationRepository = notificationRepository;
        this.logRepository = logRepository;
        this.processorService = processorService;
    }

    @Scheduled(fixedDelayString = "${creatoros.notifications.retry.poll-interval-ms:60000}")
    @Transactional
    public void pollAndRetry() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<NotificationQueueItem> due = queueRepository.findTop100ByStatusInAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                EnumSet.of(QueueStatus.PENDING, QueueStatus.RETRY),
                now
        );

        if (due.isEmpty()) {
            return;
        }

        for (NotificationQueueItem item : due) {
            Notification notification = notificationRepository.findById(item.getNotificationId()).orElse(null);
            if (notification == null) {
                item.setStatus(QueueStatus.FAILED);
                continue;
            }

            Optional<NotificationLog> recipientLog = logRepository.findTopByNotificationIdAndMessageOrderByCreatedAtDesc(
                    notification.getId(),
                    "recipient_email"
            );

            String email = recipientLog.map(l -> {
                Map<String, Object> details = l.getDetails();
                Object v = details == null ? null : details.get("email");
                return v == null ? null : String.valueOf(v);
            }).orElse(null);

            if (email == null || email.isBlank()) {
                Map<String, Object> details = new HashMap<>();
                details.put("queue_item_id", item.getId());
                details.put("notification_id", notification.getId());
                details.put("reason", "missing_recipient_email_for_retry");
                logRepository.save(new NotificationLog(notification.getId(), NotificationLogLevel.WARN, "retry_skipped", details));
                item.setStatus(QueueStatus.FAILED);
                continue;
            }

            processorService.processQueueItem(item.getId(), email);
        }

        log.info("notification_retry_poll processed={}", due.size());
    }
}
