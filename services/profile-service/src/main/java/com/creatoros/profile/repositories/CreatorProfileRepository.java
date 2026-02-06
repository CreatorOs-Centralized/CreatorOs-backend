package com.creatoros.profile.repositories;

import com.creatoros.profile.entities.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Creator Profile Repository
 * 
 * Data access layer for CreatorProfile entity.
 */
@Repository
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, UUID> {

    /**
     * Find a profile by user ID
     */
    Optional<CreatorProfile> findByUserId(UUID userId);

    /**
     * Find a profile by username
     */
    Optional<CreatorProfile> findByUsername(String username);

    /**
     * Check if a profile exists for a user
     */
    boolean existsByUserId(UUID userId);

    /**
     * Check if a username is already taken
     */
    boolean existsByUsername(String username);
}
