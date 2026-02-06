package com.creatoros.assetservice.repository;

import com.creatoros.assetservice.model.AssetFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetFolderRepository extends JpaRepository<AssetFolder, UUID> {
    List<AssetFolder> findByUserIdAndParentFolderId(UUID userId, UUID parentFolderId);
    List<AssetFolder> findByUserIdAndParentFolderIdIsNull(UUID userId);
}
