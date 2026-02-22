package com.creatoros.profile.controllers;

import com.creatoros.profile.dtos.request.CreateProfileRequest;
import com.creatoros.profile.dtos.response.ProfileResponse;
import com.creatoros.profile.entities.CreatorProfile;
import com.creatoros.profile.mappers.ProfileMapper;
import com.creatoros.profile.services.ProfileService;
import com.creatoros.profile.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Profile Controller
 * 
 * REST API endpoints for managing creator profiles.
 * 
 * NOTE: @RequestMapping is "/" because the API Gateway strips "/profiles" prefix via stripPrefix(1).
 * External: POST http://localhost:8080/profiles/me
 * → Gateway strips "/profiles" → Internal: POST /me
 * → Matches @PostMapping("me")
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Creator Profile Management APIs")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    /**
     * Get logged-in user's profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get My Profile", description = "Get the profile of the authenticated user")
    public ProfileResponse getMyProfile() {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.debug("Fetching profile for user: {}", userId);
        
        CreatorProfile profile = profileService.getMyProfile(userId);
        return ProfileMapper.toResponse(profile);
    }

    /**
     * Create or update logged-in user's profile
     */
    @PostMapping("/me")
    @Operation(summary = "Create or Update Profile", description = "Create or update the profile of the authenticated user")
    public ProfileResponse createOrUpdateProfile(@Valid @RequestBody CreateProfileRequest request) {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Creating or updating profile for user: {}", userId);
        
        CreatorProfile entity = ProfileMapper.toEntity(request);
        CreatorProfile savedProfile = profileService.createOrUpdateProfile(userId, entity);
        
        return ProfileMapper.toResponse(savedProfile);
    }

    /**
     * Get public profile by username
     */
    @GetMapping("/{username}")
    @Operation(summary = "Get Public Profile", description = "Get a public profile by username")
    public ProfileResponse getPublicProfile(@PathVariable String username) {
        logger.debug("Fetching public profile by username: {}", username);
        
        CreatorProfile profile = profileService.getPublicProfile(username);
        return ProfileMapper.toResponse(profile);
    }

    /**
     * Delete the authenticated user's profile
     */
    @DeleteMapping("/me")
    @Operation(summary = "Delete My Profile", description = "Delete the profile of the authenticated user")
    public void deleteMyProfile() {
        UUID userId = UserContextUtil.getCurrentUserId();
        logger.info("Deleting profile for user: {}", userId);
        
        profileService.deleteProfile(userId);
    }

    /**
     * Check if profile exists for the authenticated user
     */
    @GetMapping("/me/exists")
    @Operation(summary = "Check Profile Exists", description = "Check if a profile exists for the authenticated user")
    public boolean profileExists() {
        UUID userId = UserContextUtil.getCurrentUserId();
        return profileService.profileExists(userId);
    }
}
