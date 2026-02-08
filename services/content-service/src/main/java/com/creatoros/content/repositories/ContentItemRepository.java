package com.creatoros.content.repositories;

import com.creatoros.content.entities.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findByUserId(UUID userId);

    Optional<ContentItem> findByIdAndUserId(UUID id, UUID userId);
}
