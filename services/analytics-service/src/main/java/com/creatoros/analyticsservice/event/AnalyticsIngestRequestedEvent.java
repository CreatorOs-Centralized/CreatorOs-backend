package com.creatoros.analyticsservice.event;

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
public class AnalyticsIngestRequestedEvent {
    private UUID eventId;
    private UUID userId;
    private String platform;
    private String platformPostId;
    private LocalDateTime requestedAt;
}
