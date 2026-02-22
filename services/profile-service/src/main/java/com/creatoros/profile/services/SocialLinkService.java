package com.creatoros.profile.services;

import com.creatoros.profile.entities.CreatorProfile;
import com.creatoros.profile.entities.SocialLink;
import com.creatoros.profile.exceptions.ResourceNotFoundException;
import com.creatoros.profile.repositories.CreatorProfileRepository;
import com.creatoros.profile.repositories.SocialLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Social Link Service
 * 
 * Business logic for managing social links associated with creator profiles.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SocialLinkService {

    private static final Logger logger = LoggerFactory.getLogger(SocialLinkService.class);

    private final SocialLinkRepository socialLinkRepository;
    private final CreatorProfileRepository profileRepository;

    /**
     * Add a social link to a profile
     */
    public SocialLink addSocialLink(UUID userId, SocialLink socialLink) {
        logger.info("Adding social link for user: {}", userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        profile.addSocialLink(socialLink);
        SocialLink savedLink = socialLinkRepository.save(socialLink);
        
        logger.info("Social link added successfully with ID: {}", savedLink.getId());
        return savedLink;
    }

    /**
     * Get all social links for a user's profile
     */
    @Transactional(readOnly = true)
    public List<SocialLink> getSocialLinks(UUID userId) {
        logger.debug("Fetching social links for user: {}", userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return socialLinkRepository.findByProfile_IdOrderByCreatedAtAsc(profile.getId());
    }

    /**
     * Get social links by profile ID
     */
    @Transactional(readOnly = true)
    public List<SocialLink> getSocialLinksByProfileId(UUID profileId) {
        logger.debug("Fetching social links for profile: {}", profileId);

        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("Profile not found: " + profileId);
        }

        return socialLinkRepository.findByProfile_IdOrderByCreatedAtAsc(profileId);
    }

    /**
     * Update a social link
     */
    public SocialLink updateSocialLink(UUID userId, UUID linkId, SocialLink updatedLink) {
        logger.info("Updating social link: {} for user: {}", linkId, userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        SocialLink socialLink = socialLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found: " + linkId));

        // Verify the social link belongs to the user's profile
        if (!socialLink.getProfile().getId().equals(profile.getId())) {
            throw new ResourceNotFoundException("Social link not found for this user");
        }

        socialLink.setPlatform(updatedLink.getPlatform());
        socialLink.setHandle(updatedLink.getHandle());
        socialLink.setUrl(updatedLink.getUrl());
        
        SocialLink saved = socialLinkRepository.save(socialLink);
        logger.info("Social link updated successfully: {}", saved.getId());
        
        return saved;
    }

    /**
     * Delete a social link
     */
    public void deleteSocialLink(UUID userId, UUID linkId) {
        logger.info("Deleting social link: {} for user: {}", linkId, userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        SocialLink socialLink = socialLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found: " + linkId));

        // Verify the social link belongs to the user's profile
        if (!socialLink.getProfile().getId().equals(profile.getId())) {
            throw new ResourceNotFoundException("Social link not found for this user");
        }

        socialLinkRepository.delete(socialLink);
        logger.info("Social link deleted successfully: {}", linkId);
    }

    /**
     * Delete all social links for a user
     */
    public void deleteAllSocialLinks(UUID userId) {
        logger.info("Deleting all social links for user: {}", userId);

        CreatorProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        socialLinkRepository.deleteByProfile_Id(profile.getId());
        logger.info("All social links deleted for user: {}", userId);
    }
}
