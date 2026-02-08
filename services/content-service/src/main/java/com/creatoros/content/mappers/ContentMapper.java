package com.creatoros.content.mappers;

import com.creatoros.content.dtos.response.ContentResponse;
import com.creatoros.content.dtos.response.ContentVersionResponse;
import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.entities.ContentVersion;

public final class ContentMapper {

    private ContentMapper() {
    }

    public static ContentResponse toResponse(ContentItem item) {
        return ContentResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .contentType(item.getContentType())
                .workflowState(item.getWorkflowState() != null ? item.getWorkflowState().getName() : null)
                .scheduledAt(item.getScheduledAt())
                .publishedAt(item.getPublishedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static ContentVersionResponse toVersionResponse(ContentVersion version) {
        return ContentVersionResponse.builder()
                .id(version.getId())
                .contentItemId(version.getContentItem() != null ? version.getContentItem().getId() : null)
                .versionNumber(version.getVersionNumber())
                .body(version.getBody())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
