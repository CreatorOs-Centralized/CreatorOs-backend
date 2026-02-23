package com.creatoros.assetservice.repository;

import com.creatoros.assetservice.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByUserIdAndFolderId(UUID userId, UUID folderId);
    Optional<MediaFile> findByIdAndUserId(UUID id, UUID userId);
}
