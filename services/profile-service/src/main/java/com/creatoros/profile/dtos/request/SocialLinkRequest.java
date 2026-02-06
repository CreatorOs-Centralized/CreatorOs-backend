package com.creatoros.profile.dtos.request;

import com.creatoros.profile.entities.SocialLink.SocialPlatform;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Social Link Request DTO
 * 
 * Request payload for creating or updating a social link.
 */
@Getter
@Setter
public class SocialLinkRequest {

    @NotNull(message = "Platform is required")
    private SocialPlatform platform;

    @Size(max = 100, message = "Handle must not exceed 100 characters")
    private String handle;

    @Size(max = 500, message = "URL must not exceed 500 characters")
    private String url;
}
