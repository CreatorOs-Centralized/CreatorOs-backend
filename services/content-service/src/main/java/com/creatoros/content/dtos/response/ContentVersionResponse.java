package com.creatoros.content.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContentVersionResponse {

    private UUID id;
    private UUID contentItemId;
    private int versionNumber;
    private String body;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
