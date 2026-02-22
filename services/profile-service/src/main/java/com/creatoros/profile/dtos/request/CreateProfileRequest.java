package com.creatoros.profile.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Create Profile Request DTO
 * 
 * Request payload for creating or updating a creator profile.
 */
@Getter
@Setter
public class CreateProfileRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 100, message = "Niche must not exceed 100 characters")
    private String niche;

    private String profilePhotoUrl;
    private String coverPhotoUrl;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @JsonProperty("isPublic")
    private boolean isPublic = true;
}
