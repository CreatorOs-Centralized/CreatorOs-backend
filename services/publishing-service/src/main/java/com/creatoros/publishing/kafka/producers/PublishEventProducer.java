package com.creatoros.publishing.kafka.producers;

import com.creatoros.publishing.models.PublishFailedEvent;
import com.creatoros.publishing.models.PublishRetryRequestedEvent;
import com.creatoros.publishing.models.PublishStartedEvent;
import com.creatoros.publishing.models.PublishSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStarted(UUID userId, String email, UUID publishJobId, String platform) {
        PublishStartedEvent event = PublishStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .email(email)
                .eventCreatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("publish.started", event);
    }

    public void publishSuccess(UUID userId, String email, UUID publishJobId, String platform, String platformPostId, String permalink) {
        PublishSucceededEvent event = PublishSucceededEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .email(email)
                .platformPostId(platformPostId)
                .permalink(permalink)
                .publishedAt(LocalDateTime.now())
                .eventCreatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("publish.succeeded", event);
    }

    public void publishFailed(UUID userId, String email, UUID publishJobId, String platform, String error) {
        PublishFailedEvent event = PublishFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .email(email)
                .error(error)
                .eventCreatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("publish.failed", event);
    }

    public void publishRetryRequested(UUID userId, String email, UUID publishJobId, String platform, String reason) {
        PublishRetryRequestedEvent event = PublishRetryRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .email(email)
                .reason(reason)
                .eventCreatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("publish.retry.requested", event);
    }
}

