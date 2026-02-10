package com.creatoros.publishing.kafka.producers;

import com.creatoros.publishing.models.PublishSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishEventProducer {

    private final KafkaTemplate<String, PublishSucceededEvent> kafkaTemplate;

    public void publishSuccess(UUID userId, UUID publishJobId, String platform, String platformPostId, String permalink) {
        PublishSucceededEvent event = PublishSucceededEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .publishJobId(publishJobId)
                .platform(platform)
                .platformPostId(platformPostId)
                .permalink(permalink)
                .publishedAt(LocalDateTime.now())
                .eventCreatedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send("publish.succeeded", event);
    }
}

