package com.creatoros.content.repositories;

import com.creatoros.content.entities.ContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentVersionRepository extends JpaRepository<ContentVersion, UUID> {

    List<ContentVersion> findByContentItemIdOrderByVersionNumberDesc(UUID contentItemId);

    Optional<ContentVersion> findTopByContentItemIdOrderByVersionNumberDesc(UUID contentItemId);
}
