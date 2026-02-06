package com.creatoros.assetservice.repository;

import com.creatoros.assetservice.model.MediaMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaMetadataRepository extends JpaRepository<MediaMetadata, UUID> {
    Optional<MediaMetadata> findByMediaFileId(UUID mediaFileId);
}
