package com.creatoros.profile.controllers;

import com.creatoros.profile.dtos.request.SocialLinkRequest;
import com.creatoros.profile.dtos.response.SocialLinkResponse;
import com.creatoros.profile.entities.SocialLink;
import com.creatoros.profile.mappers.ProfileMapper;
import com.creatoros.profile.services.SocialLinkService;
import com.creatoros.profile.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Social Link Controller
 * 
 * REST API endpoints for managing social links associated with creator profiles.
 */
@RestController
@RequestMapping("/me/social-links")
@RequiredArgsConstructor
@Tag(name = "Social Links", description = "Social Link Management APIs")
public class SocialLinkController {

    private static final Logger logger = LoggerFactory.getLogger(SocialLinkController.class);

    private final SocialLinkService socialLinkService;

    /**
     * Add a social link to the authenticated user's profile
     */
    @PostMapping
    @Operation(summary = "Add Social Link", description = "Add a new social link to the authenticated user's profile")
    public SocialLinkResponse addSocialLink(@Valid @RequestBody SocialLinkRequest request) {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Adding social link for user: {}", userId);
        
        SocialLink entity = ProfileMapper.toSocialLinkEntity(request);
        SocialLink savedLink = socialLinkService.addSocialLink(userId, entity);
        
        return ProfileMapper.toSocialLinkResponse(savedLink);
    }

    /**
     * Get all social links for the authenticated user's profile
     */
    @GetMapping
    @Operation(summary = "Get My Social Links", description = "Get all social links for the authenticated user's profile")
    public List<SocialLinkResponse> getMySocialLinks() {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.debug("Fetching social links for user: {}", userId);
        
        List<SocialLink> socialLinks = socialLinkService.getSocialLinks(userId);
        return socialLinks.stream()
                .map(ProfileMapper::toSocialLinkResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a social link
     */
    @PutMapping("/{linkId}")
    @Operation(summary = "Update Social Link", description = "Update a social link for the authenticated user")
    public SocialLinkResponse updateSocialLink(
            @PathVariable UUID linkId,
            @Valid @RequestBody SocialLinkRequest request) {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Updating social link: {} for user: {}", linkId, userId);
        
        SocialLink entity = ProfileMapper.toSocialLinkEntity(request);
        SocialLink updatedLink = socialLinkService.updateSocialLink(userId, linkId, entity);
        
        return ProfileMapper.toSocialLinkResponse(updatedLink);
    }

    /**
     * Delete a social link
     */
    @DeleteMapping("/{linkId}")
    @Operation(summary = "Delete Social Link", description = "Delete a social link for the authenticated user")
    public void deleteSocialLink(@PathVariable UUID linkId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Deleting social link: {} for user: {}", linkId, userId);
        
        socialLinkService.deleteSocialLink(userId, linkId);
    }

    /**
     * Delete all social links
     */
    @DeleteMapping
    @Operation(summary = "Delete All Social Links", description = "Delete all social links for the authenticated user")
    public void deleteAllSocialLinks() {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Deleting all social links for user: {}", userId);
        
        socialLinkService.deleteAllSocialLinks(userId);
    }

    /**
     * Get social links by profile ID (public endpoint)
     */
    @GetMapping("/profile/{profileId}")
    @Operation(summary = "Get Social Links by Profile ID", description = "Get all social links for a specific profile")
    public List<SocialLinkResponse> getSocialLinksByProfileId(@PathVariable UUID profileId) {
        logger.debug("Fetching social links for profile: {}", profileId);
        
        List<SocialLink> socialLinks = socialLinkService.getSocialLinksByProfileId(profileId);
        return socialLinks.stream()
                .map(ProfileMapper::toSocialLinkResponse)
                .collect(Collectors.toList());
    }
}
