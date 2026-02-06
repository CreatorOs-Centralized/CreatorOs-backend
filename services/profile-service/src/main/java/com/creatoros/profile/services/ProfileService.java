package com.creatoros.profile.services;

import com.creatoros.profile.entities.CreatorProfile;
import com.creatoros.profile.exceptions.BadRequestException;
import com.creatoros.profile.exceptions.ResourceNotFoundException;
import com.creatoros.profile.repositories.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Profile Service
 * 
 * Business logic for managing creator profiles.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final CreatorProfileRepository profileRepository;

    /**
     * Get profile for the current user
     */
    @Transactional(readOnly = true)
    public CreatorProfile getMyProfile(UUID userId) {
        logger.debug("Fetching profile for user: {}", userId);
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
    }

    /**
     * Create or update profile for the current user
     */
    public CreatorProfile createOrUpdateProfile(UUID userId, CreatorProfile profile) {
        logger.info("Creating or updating profile for user: {}", userId);

        return profileRepository.findByUserId(userId)
                .map(existingProfile -> {
                    // Update existing profile
                    logger.info("Updating existing profile: {}", existingProfile.getId());
                    
                    // Check if username is being changed and if it's already taken
                    if (!profile.getUsername().equals(existingProfile.getUsername()) &&
                        profileRepository.existsByUsername(profile.getUsername())) {
                        throw new BadRequestException("Username already taken: " + profile.getUsername());
                    }
                    
                    existingProfile.setUsername(profile.getUsername());
                    existingProfile.setDisplayName(profile.getDisplayName());
                    existingProfile.setBio(profile.getBio());
                    existingProfile.setNiche(profile.getNiche());
                    existingProfile.setProfilePhotoUrl(profile.getProfilePhotoUrl());
                    existingProfile.setCoverPhotoUrl(profile.getCoverPhotoUrl());
                    existingProfile.setLocation(profile.getLocation());
                    existingProfile.setLanguage(profile.getLanguage());
                    existingProfile.setPublic(profile.isPublic());
                    
                    return profileRepository.save(existingProfile);
                })
                .orElseGet(() -> {
                    // Create new profile
                    logger.info("Creating new profile for user: {}", userId);
                    
                    // Check if username is already taken
                    if (profileRepository.existsByUsername(profile.getUsername())) {
                        throw new BadRequestException("Username already taken: " + profile.getUsername());
                    }
                    
                    profile.setUserId(userId);
                    return profileRepository.save(profile);
                });
    }

    /**
     * Get public profile by username
     */
    @Transactional(readOnly = true)
    public CreatorProfile getPublicProfile(String username) {
        logger.debug("Fetching public profile by username: {}", username);
        
        CreatorProfile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for username: " + username));
        
        // Check if profile is public
        if (!profile.isPublic()) {
            throw new ResourceNotFoundException("Profile is not public");
        }
        
        return profile;
    }

    /**
     * Get profile by profile ID
     */
    @Transactional(readOnly = true)
    public CreatorProfile getProfileById(UUID profileId) {
        logger.debug("Fetching profile by ID: {}", profileId);
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));
    }

    /**
     * Delete profile
     */
    public void deleteProfile(UUID userId) {
        logger.info("Deleting profile for user: {}", userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        profileRepository.delete(profile);
        logger.info("Profile deleted successfully for user: {}", userId);
    }

    /**
     * Check if profile exists for user
     */
    @Transactional(readOnly = true)
    public boolean profileExists(UUID userId) {
        return profileRepository.existsByUserId(userId);
    }
}
