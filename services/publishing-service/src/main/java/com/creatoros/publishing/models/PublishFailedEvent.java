package com.creatoros.publishing.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishFailedEvent {

    private UUID eventId;
    private UUID userId;
    private UUID publishJobId;
    private String platform;
    private String email;
    private String error;
    private LocalDateTime eventCreatedAt;
}
