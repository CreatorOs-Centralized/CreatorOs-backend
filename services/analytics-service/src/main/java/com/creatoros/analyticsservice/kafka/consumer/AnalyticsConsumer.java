package com.creatoros.analyticsservice.kafka.consumer;

import com.creatoros.analyticsservice.event.AnalyticsIngestRequestedEvent;
import com.creatoros.analyticsservice.event.PublishSucceededEvent;
import com.creatoros.analyticsservice.kafka.producer.AnalyticsProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsProducer analyticsProducer;

    @KafkaListener(topics = "${creatoros.kafka.topics.publish-succeeded:publish.succeeded}", groupId = "${spring.application.name}")
    public void consumePublishSucceeded(PublishSucceededEvent event) {
        log.info("Received PublishSucceededEvent: {}", event);

        AnalyticsIngestRequestedEvent ingestEvent = AnalyticsIngestRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(event.getUserId())
                .platform(event.getPlatform())
                .platformPostId(event.getPlatformPostId())
                .requestedAt(LocalDateTime.now())
                .build();

        analyticsProducer.sendAnalyticsIngestRequested(ingestEvent);
    }
}
