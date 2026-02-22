package com.creatoros.publishing.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "publish_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID connectedAccountId;

    @Column(nullable = false)
    private UUID contentItemId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String postType;

    @Column(nullable = false)
    private String status;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer maxRetries = 3;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer currentRetryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> payloadSnapshot;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
