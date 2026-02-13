package com.creatoros.scheduler.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ScheduleRequest {
    private UUID contentItemId;
    private UUID connectedAccountId;
    private String platform;
    private LocalDateTime scheduledAt;
}
