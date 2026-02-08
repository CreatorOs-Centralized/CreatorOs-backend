package com.creatoros.profile.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Social Link Entity
 * 
 * Represents a social media link associated with a creator's profile.
 */
@Entity
@Table(name = "social_links")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_profile_id", nullable = false)
    private CreatorProfile profile;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SocialPlatform platform;

    @Column(length = 100)
    private String handle;

    @Column(length = 500)
    private String url;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Social Platform Enum
     */
    public enum SocialPlatform {
        INSTAGRAM,
        YOUTUBE,
        LINKEDIN,
        TWITTER,
        FACEBOOK,
        WEBSITE
    }
}
