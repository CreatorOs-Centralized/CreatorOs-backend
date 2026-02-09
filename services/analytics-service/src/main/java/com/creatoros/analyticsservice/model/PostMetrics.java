package com.creatoros.analyticsservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_metrics")
public class PostMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @io.swagger.v3.oas.annotations.media.Schema(accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID publishJobId;

    private UUID publishedPostId;

    @Column(nullable = false)
    private String platform;

    private String platformPostId;

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

    @Builder.Default
    private Double avgViewDurationSeconds = 0.0;

    private LocalDateTime fetchedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
