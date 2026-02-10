package com.creatoros.publishing.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PublishRequestEvent {

    private UUID eventId;
    private UUID userId;
    private UUID contentItemId;
    private UUID connectedAccountId;
    private String platform;
    private LocalDateTime scheduledAt;
}
