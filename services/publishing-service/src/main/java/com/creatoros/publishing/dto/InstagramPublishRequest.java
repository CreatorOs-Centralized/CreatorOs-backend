package com.creatoros.publishing.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class InstagramPublishRequest {
    private UUID accountId;
    private String caption;
    private String mediaType;
    private String imageUrl;
    private String videoUrl;
    private List<String> children;
    private Boolean isCarouselItem;
    private String thumbOffset;
    private String altText;
}