package  com.creatoros.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.creatoros.notification.service.NotificationConsumerService;

@Component
public class NotificationKafkaConsumer {

    private final NotificationConsumerService consumerService;

    public NotificationKafkaConsumer(NotificationConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @KafkaListener(
            topics = "${creatoros.kafka.topics.publish-succeeded:publish.succeeded}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:notification-service}"
    )
    public void onPublishSucceeded(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        consumerService.consume(topic, message);
    }

    @KafkaListener(
            topics = "${creatoros.kafka.topics.publish-failed:publish.failed}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:notification-service}"
    )
    public void onPublishFailed(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        consumerService.consume(topic, message);
    }

    @KafkaListener(
            topics = "${creatoros.kafka.topics.publish-retry-requested:publish.retry.requested}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:notification-service}"
    )
    public void onPublishRetryRequested(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        consumerService.consume(topic, message);
    }

    @KafkaListener(
            topics = "${creatoros.kafka.topics.notification-send-requested:notification.send.requested}",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:notification-service}"
    )
    public void onNotificationSendRequested(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        consumerService.consume(topic, message);
    }
}
