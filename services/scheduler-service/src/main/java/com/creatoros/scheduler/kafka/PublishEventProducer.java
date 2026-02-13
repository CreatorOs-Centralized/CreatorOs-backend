package com.creatoros.scheduler.kafka;

import com.creatoros.scheduler.entities.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublishEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPublishRequested(ScheduledJob job) {

        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID(),
                "userId", job.getUserId(),
                "contentItemId", job.getContentItemId(),
                "connectedAccountId", job.getConnectedAccountId(),
                "platform", job.getPlatform()
        );

        log.info("Sending publish.requested event for job {}: {}", job.getId(), event);

        kafkaTemplate.send("publish.requested", event);
    }
}
