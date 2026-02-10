package com.creatoros.publishing.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "connected_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private String platformAccountId;

    private String pageId;

    private String igUserId;

    private String youtubeChannelId;

    private String linkedinAuthorUrn;

    @Column(columnDefinition = "TEXT")
    private String accessTokenEnc;

    @Column(columnDefinition = "TEXT")
    private String refreshTokenEnc;

    private LocalDateTime tokenExpiresAt;

    @Column(columnDefinition = "TEXT")
    private String scopes;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean isActive = true;

    private LocalDateTime connectedAt;

    private LocalDateTime updatedAt;
}
