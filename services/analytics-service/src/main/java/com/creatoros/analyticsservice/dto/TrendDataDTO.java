package com.creatoros.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDTO {
    private LocalDate date;
    private Long views;
    private Long likes;
    private Long comments;
    private Long shares;
}
