package com.creatoros.analyticsservice.kafka.producer;

import com.creatoros.analyticsservice.event.AnalyticsIngestRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAnalyticsIngestRequested(AnalyticsIngestRequestedEvent event) {
        log.info("Sending AnalyticsIngestRequestedEvent: {}", event);
        kafkaTemplate.send("analytics.ingest.requested", event);
    }
}
