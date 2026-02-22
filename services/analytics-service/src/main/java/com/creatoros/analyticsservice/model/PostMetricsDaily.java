package com.creatoros.analyticsservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_metrics_daily")
public class PostMetricsDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID publishedPostId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    private Long views = 0L;

    @Builder.Default
    private Long likes = 0L;

    @Builder.Default
    private Long comments = 0L;

    @Builder.Default
    private Long shares = 0L;

    @Builder.Default
    private Long saves = 0L;

    @Builder.Default
    private Long impressions = 0L;

    @Builder.Default
    private Long reach = 0L;

    @Builder.Default
    private Double watchTimeSeconds = 0.0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
