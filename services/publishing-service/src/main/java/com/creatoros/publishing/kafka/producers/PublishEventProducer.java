package com.creatoros.publishing.kafka.producers;

import com.creatoros.publishing.models.PublishFailedEvent;
import com.creatoros.publishing.models.PublishRetryRequestedEvent;
import com.creatoros.publishing.models.PublishStartedEvent;
import com.creatoros.publishing.models.PublishSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${creatoros.kafka.topics.publish-started:publish.started}")
    private String publishStartedTopic;

    @Value("${creatoros.kafka.topics.publish-succeeded:publish.succeeded}")
    private String publishSucceededTopic;

    @Value("${creatoros.kafka.topics.publish-failed:publish.failed}")
    private String publishFailedTopic;

    @Value("${creatoros.kafka.topics.publish-retry-requested:publish.retry.requested}")
    private String publishRetryRequestedTopic;

    public void publishStarted(UUID userId, String email, UUID publishJobId, String platform) {
        PublishStartedEvent event = PublishStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .email(email)
                .eventCreatedAt(LocalDateTime.now())
                .build();

            kafkaTemplate.send(publishStartedTopic, event);
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

            kafkaTemplate.send(publishSucceededTopic, event);
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

            kafkaTemplate.send(publishFailedTopic, event);
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

            kafkaTemplate.send(publishRetryRequestedTopic, event);
    }
}

