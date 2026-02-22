package com.creatoros.publishing.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PublishRequestEvent {

    private UUID eventId;
    private UUID userId;
    private UUID contentItemId;
    private UUID connectedAccountId;
    private String platform;
    private String postType;
    private LocalDateTime scheduledAt;
    private String email;
    private String title;
    private String description;
    private String gcsPath;
    private String privacyStatus;
    private List<String> tags;
    private String categoryId;
}
