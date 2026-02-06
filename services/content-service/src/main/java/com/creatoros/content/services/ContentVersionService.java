package com.creatoros.content.services;

import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.entities.ContentVersion;
import com.creatoros.content.exceptions.ResourceNotFoundException;
import com.creatoros.content.repositories.ContentItemRepository;
import com.creatoros.content.repositories.ContentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentVersionService {

    private final ContentItemRepository contentItemRepository;
    private final ContentVersionRepository contentVersionRepository;

    public ContentVersion createVersion(UUID userId, UUID contentId, String body) {
        ContentItem item = contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item not found"));

        int nextVersion = contentVersionRepository
                .findByContentItemIdOrderByVersionNumberDesc(contentId)
                .stream()
                .findFirst()
                .map(ContentVersion::getVersionNumber)
                .orElse(0) + 1;

        ContentVersion version = ContentVersion.builder()
                .contentItem(item)
                .versionNumber(nextVersion)
                .body(body)
                .createdBy(userId)
                .build();

        return contentVersionRepository.save(version);
    }

    public List<ContentVersion> getVersions(UUID userId, UUID contentId) {
        contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item not found"));

        return contentVersionRepository.findByContentItemIdOrderByVersionNumberDesc(contentId);
    }
}
