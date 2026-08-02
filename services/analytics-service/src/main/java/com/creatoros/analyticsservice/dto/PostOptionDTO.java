package com.creatoros.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostOptionDTO {
    private UUID postId;
    private String platformPostId;
    private String title;
    private String platform;
}
