package com.creatoros.analyticsservice.kafka.producer;

import com.creatoros.analyticsservice.event.AnalyticsIngestRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${creatoros.kafka.topics.analytics-ingest-requested:analytics.ingest.requested}")
    private String analyticsIngestRequestedTopic;

    public void sendAnalyticsIngestRequested(AnalyticsIngestRequestedEvent event) {
        log.info("Sending AnalyticsIngestRequestedEvent: {}", event);
        kafkaTemplate.send(analyticsIngestRequestedTopic, event);
    }
}
