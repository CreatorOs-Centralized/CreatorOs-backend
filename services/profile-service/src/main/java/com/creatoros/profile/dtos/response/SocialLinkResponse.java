package com.creatoros.profile.dtos.response;

import com.creatoros.profile.entities.SocialLink.SocialPlatform;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Social Link Response DTO
 * 
 * Response payload containing social link information.
 */
@Getter
@Builder
public class SocialLinkResponse {

    private UUID id;
    private SocialPlatform platform;
    private String handle;
    private String url;
    private boolean isVerified;
    private LocalDateTime createdAt;
}
