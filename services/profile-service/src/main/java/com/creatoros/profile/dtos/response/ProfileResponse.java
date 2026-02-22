package com.creatoros.profile.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Profile Response DTO
 * 
 * Response payload containing creator profile information.
 */
@Getter
@Builder
public class ProfileResponse {

    private UUID id;
    private UUID userId;

    private String username;
    private String displayName;
    private String bio;
    private String niche;

    private String profilePhotoUrl;
    private String coverPhotoUrl;

    private String location;
    private String language;

    @JsonProperty("isPublic")
    private boolean isPublic;
    @JsonProperty("isVerified")
    private boolean isVerified;
    private String verificationLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
