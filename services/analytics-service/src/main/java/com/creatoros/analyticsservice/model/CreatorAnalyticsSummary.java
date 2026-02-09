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
@Table(name = "creator_analytics_summary")
public class CreatorAnalyticsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @io.swagger.v3.oas.annotations.media.Schema(accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private LocalDate rangeStart;

    @Column(nullable = false)
    private LocalDate rangeEnd;

    @Builder.Default
    private Long totalViews = 0L;

    @Builder.Default
    private Long totalLikes = 0L;

    @Builder.Default
    private Long totalComments = 0L;

    @Builder.Default
    private Long totalShares = 0L;

    @Builder.Default
    private Long totalPosts = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
