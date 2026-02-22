package com.creatoros.content.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PlatformVariantResponse {

    private UUID id;
    private String platform;
    private String variantType;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
