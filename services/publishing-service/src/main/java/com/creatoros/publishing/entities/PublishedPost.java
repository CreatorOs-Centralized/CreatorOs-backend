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
@Table(name = "published_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID publishJobId;

    @Column(nullable = false)
    private UUID connectedAccountId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String platformPostId;

    private String platformMediaId;

    @Column(nullable = false)
    private String permalinkUrl;

    @Column(columnDefinition = "TEXT")
    private String logLevel;

    @Column(columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> details;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;
}
