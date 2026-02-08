package com.creatoros.content.repositories;

import com.creatoros.content.entities.ContentPlatformVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPlatformVariantRepository extends JpaRepository<ContentPlatformVariant, UUID> {

    List<ContentPlatformVariant> findByContentItemId(UUID contentItemId);

    Optional<ContentPlatformVariant> findByContentItemIdAndPlatformAndVariantType(
            UUID contentItemId, String platform, String variantType);
}
