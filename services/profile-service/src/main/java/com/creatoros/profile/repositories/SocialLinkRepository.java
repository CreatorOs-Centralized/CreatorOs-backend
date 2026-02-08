package com.creatoros.profile.repositories;

import com.creatoros.profile.entities.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Social Link Repository
 * 
 * Data access layer for SocialLink entity.
 */
@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, UUID> {

    /**
     * Find all social links for a specific profile
     */
    List<SocialLink> findByProfile_IdOrderByCreatedAtAsc(UUID profileId);

    /**
     * Delete all social links for a specific profile
     */
    void deleteByProfile_Id(UUID profileId);

    /**
     * Count social links for a specific profile
     */
    long countByProfile_Id(UUID profileId);
}
