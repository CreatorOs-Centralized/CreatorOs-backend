package com.creatoros.content.services;

import com.creatoros.content.dtos.request.UpdateContentRequest;
import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.entities.WorkflowState;
import com.creatoros.content.exceptions.ResourceNotFoundException;
import com.creatoros.content.repositories.ContentItemRepository;
import com.creatoros.content.repositories.WorkflowStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentItemRepository contentItemRepository;
    private final WorkflowStateRepository workflowStateRepository;

    public ContentItem createContent(UUID userId, String title, String type) {
        WorkflowState draft = workflowStateRepository.findByName("DRAFT")
                .orElseThrow(() -> new ResourceNotFoundException("Workflow state DRAFT not found"));

        ContentItem item = ContentItem.builder()
                .userId(userId)
                .title(title)
                .contentType(type)
                .workflowState(draft)
                .build();

        return contentItemRepository.save(item);
    }

    public List<ContentItem> getMyContents(UUID userId) {
        return contentItemRepository.findByUserId(userId);
    }

    public ContentItem getContentById(UUID userId, UUID contentId) {
        return contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item not found"));
    }

    public ContentItem updateContent(UUID userId, UUID contentId, UpdateContentRequest request) {
        ContentItem item = contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item not found"));

        item.setTitle(request.getTitle());
        item.setContentType(request.getContentType());
        return contentItemRepository.save(item);
    }

    public void deleteContent(UUID userId, UUID contentId) {
        ContentItem item = contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content item not found"));

        contentItemRepository.delete(item);
    }
}
