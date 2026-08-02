package com.creatoros.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformComparisonDTO {
    private String platform;
    private Long views;
    private Long likes;
    private Long comments;
    private Long shares;
}
