package com.creatoros.profile.mappers;

import com.creatoros.profile.dtos.request.CreateProfileRequest;
import com.creatoros.profile.dtos.request.SocialLinkRequest;
import com.creatoros.profile.dtos.response.ProfileResponse;
import com.creatoros.profile.dtos.response.SocialLinkResponse;
import com.creatoros.profile.entities.CreatorProfile;
import com.creatoros.profile.entities.SocialLink;

/**
 * Profile Mapper
 * 
 * Maps between entities and DTOs for profile-related data.
 */
public class ProfileMapper {

    /**
     * Convert CreateProfileRequest to CreatorProfile entity
     */
    public static CreatorProfile toEntity(CreateProfileRequest request) {
        return CreatorProfile.builder()
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .bio(request.getBio())
                .niche(request.getNiche())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .coverPhotoUrl(request.getCoverPhotoUrl())
                .location(request.getLocation())
                .language(request.getLanguage())
                .isPublic(request.isPublic())
                .build();
    }

    /**
     * Update CreatorProfile entity from CreateProfileRequest
     */
    public static void updateEntityFromRequest(CreatorProfile profile, CreateProfileRequest request) {
        profile.setUsername(request.getUsername());
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setNiche(request.getNiche());
        profile.setProfilePhotoUrl(request.getProfilePhotoUrl());
        profile.setCoverPhotoUrl(request.getCoverPhotoUrl());
        profile.setLocation(request.getLocation());
        profile.setLanguage(request.getLanguage());
        profile.setPublic(request.isPublic());
    }

    /**
     * Convert CreatorProfile entity to ProfileResponse
     */
    public static ProfileResponse toResponse(CreatorProfile entity) {
        return ProfileResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .displayName(entity.getDisplayName())
                .bio(entity.getBio())
                .niche(entity.getNiche())
                .profilePhotoUrl(entity.getProfilePhotoUrl())
                .coverPhotoUrl(entity.getCoverPhotoUrl())
                .location(entity.getLocation())
                .language(entity.getLanguage())
                .isPublic(entity.isPublic())
                .isVerified(entity.isVerified())
                .verificationLevel(entity.getVerificationLevel())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert SocialLinkRequest to SocialLink entity
     */
    public static SocialLink toSocialLinkEntity(SocialLinkRequest request) {
        return SocialLink.builder()
                .platform(request.getPlatform())
                .handle(request.getHandle())
                .url(request.getUrl())
                .build();
    }

    /**
     * Update SocialLink entity from SocialLinkRequest
     */
    public static void updateSocialLinkFromRequest(SocialLink socialLink, SocialLinkRequest request) {
        socialLink.setPlatform(request.getPlatform());
        socialLink.setHandle(request.getHandle());
        socialLink.setUrl(request.getUrl());
    }

    /**
     * Convert SocialLink entity to SocialLinkResponse
     */
    public static SocialLinkResponse toSocialLinkResponse(SocialLink entity) {
        return SocialLinkResponse.builder()
                .id(entity.getId())
                .platform(entity.getPlatform())
                .handle(entity.getHandle())
                .url(entity.getUrl())
                .isVerified(entity.isVerified())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
