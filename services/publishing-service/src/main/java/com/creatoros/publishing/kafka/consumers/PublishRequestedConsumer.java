package com.creatoros.publishing.kafka.consumers;

import com.creatoros.publishing.models.PublishRequestEvent;
import com.creatoros.publishing.services.PublishExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishRequestedConsumer {

    private final PublishExecutionService executionService;

    @KafkaListener(
            topics = "${creatoros.kafka.topics.publish-requested:publish.requested}",
            groupId = "publishing-service"
    )
    public void consume(PublishRequestEvent event) {
        executionService.execute(event);
    }
}
