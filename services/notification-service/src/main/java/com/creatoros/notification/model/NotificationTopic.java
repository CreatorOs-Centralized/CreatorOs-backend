package com.creatoros.notification.model;

import java.util.Arrays;

public enum NotificationTopic {
    PUBLISH_STARTED("publish.started"),
    PUBLISH_SUCCEEDED("publish.succeeded"),
    PUBLISH_FAILED("publish.failed"),
    PUBLISH_RETRY_REQUESTED("publish.retry.requested"),
    NOTIFICATION_SEND_REQUESTED("notification.send.requested");

    private final String value;

    NotificationTopic(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static NotificationTopic fromValue(String value) {
        return Arrays.stream(values())
                .filter(topic -> topic.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
