package com.creatoros.notification.service;

import com.creatoros.notification.model.Notification;
import com.creatoros.notification.model.NotificationLog;
import com.creatoros.notification.model.NotificationLogLevel;
import com.creatoros.notification.model.NotificationStatus;
import com.creatoros.notification.model.NotificationQueueItem;
import com.creatoros.notification.model.QueueStatus;
import com.mailersend.sdk.exceptions.MailerSendException;
import com.creatoros.notification.repository.NotificationLogRepository;
import com.creatoros.notification.repository.NotificationQueueRepository;
import com.creatoros.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationProcessorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessorService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationQueueRepository queueRepository;
    private final NotificationLogRepository logRepository;
    private final EmailSenderService emailSenderService;

    private final int maxAttempts;
    private final long initialBackoffSeconds;
    private final long maxBackoffSeconds;

    public NotificationProcessorService(
            NotificationRepository notificationRepository,
            NotificationQueueRepository queueRepository,
            NotificationLogRepository logRepository,
            EmailSenderService emailSenderService,
            @Value("${creatoros.notifications.retry.max-attempts:5}") int maxAttempts,
            @Value("${creatoros.notifications.retry.initial-backoff-seconds:30}") long initialBackoffSeconds,
            @Value("${creatoros.notifications.retry.max-backoff-seconds:3600}") long maxBackoffSeconds
    ) {
        this.notificationRepository = notificationRepository;
        this.queueRepository = queueRepository;
        this.logRepository = logRepository;
        this.emailSenderService = emailSenderService;
        this.maxAttempts = maxAttempts;
        this.initialBackoffSeconds = initialBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    @Transactional
    public void processQueueItem(UUID queueItemId, String toEmail) {
        if (queueItemId == null) {
            return;
        }

        Optional<NotificationQueueItem> queueOpt = queueRepository.findById(queueItemId);
        if (queueOpt.isEmpty()) {
            return;
        }

        NotificationQueueItem queueItem = queueOpt.get();
        UUID notificationId = queueItem.getNotificationId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("Notification not found: " + notificationId));

        int attempt = queueItem.getAttempts() + 1;
        queueItem.setAttempts(attempt);

        try {
            String subject = safe(notification.getTitle());
            String body = safe(notification.getMessage());
            String messageId = emailSenderService.sendEmail(toEmail, subject, body, null);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));

            queueItem.setStatus(QueueStatus.SENT);
            queueItem.setLastErrorMessage(null);

            Map<String, Object> details = new HashMap<>();
            details.put("message_id", messageId);
            details.put("attempt", attempt);
            logRepository.save(new NotificationLog(notification.getId(), NotificationLogLevel.INFO, "email_sent", details));
        } catch (MailerSendException | RuntimeException ex) {
            String errorMessage = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            queueItem.setLastErrorMessage(errorMessage);

            Map<String, Object> details = new HashMap<>();
            details.put("attempt", attempt);
            details.put("error", errorMessage);
            details.put("exception", ex.getClass().getName());
            logRepository.save(new NotificationLog(notification.getId(), NotificationLogLevel.ERROR, "email_send_failed", details));

            if (isPermanentFailure(errorMessage)) {
                queueItem.setStatus(QueueStatus.FAILED);
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("notification_email_failed_permanently notificationId={} attempts={} error={}", notification.getId(), attempt, errorMessage);
                return;
            }

            if (attempt >= maxAttempts) {
                queueItem.setStatus(QueueStatus.FAILED);
                notification.setStatus(NotificationStatus.FAILED);
                log.warn("notification_email_failed_permanently notificationId={} attempts={} error={}", notification.getId(), attempt, errorMessage);
                return;
            }

            queueItem.setStatus(QueueStatus.RETRY);
            queueItem.setNextRetryAt(nextRetryAt(attempt));
            notification.setStatus(NotificationStatus.PENDING);
            log.warn("notification_email_failed_will_retry notificationId={} attempts={} nextRetryAt={} error={}", notification.getId(), attempt, queueItem.getNextRetryAt(), errorMessage);
        }
    }

    private OffsetDateTime nextRetryAt(int attempt) {
        long multiplier = 1L << Math.max(0, attempt - 1);
        long delaySeconds = Math.min(maxBackoffSeconds, Math.max(0L, initialBackoffSeconds) * multiplier);
        return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static boolean isPermanentFailure(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return false;
        }

        String msg = errorMessage.toLowerCase();

        // MailerSend trial limitation: retrying will not succeed until account limits change.
        if (msg.contains("#ms42225") || msg.contains("unique recipients limit")) {
            return true;
        }

        // Local configuration issues: retrying won't help.
        if (msg.contains("missing configured from email") || msg.contains("missing mailersend token") || msg.contains("missing recipient email")) {
            return true;
        }

        return false;
    }
}
