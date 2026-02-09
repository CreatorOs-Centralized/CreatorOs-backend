package com.creatoros.content.services;

import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.entities.WorkflowState;
import com.creatoros.content.exceptions.BadRequestException;
import com.creatoros.content.exceptions.ResourceNotFoundException;
import com.creatoros.content.repositories.ContentItemRepository;
import com.creatoros.content.repositories.WorkflowStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ContentItemRepository contentItemRepository;
    private final WorkflowStateRepository workflowStateRepository;

    private static final Map<String, String> ALLOWED_TRANSITIONS = Map.of(
            "IDEA", "DRAFT",
            "DRAFT", "REVIEW",
            "REVIEW", "APPROVED",
            "APPROVED", "SCHEDULED",
            "SCHEDULED", "PUBLISHED"
    );

    public ContentItem moveToNextState(UUID contentId, UUID userId) {
        ContentItem item = contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));

        String current = item.getWorkflowState().getName();
        String next = ALLOWED_TRANSITIONS.get(current);

        if (next == null) {
            throw new BadRequestException("Invalid workflow transition from " + current);
        }

        WorkflowState nextState = workflowStateRepository.findByName(next)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow state not found"));

        item.setWorkflowState(nextState);
        return contentItemRepository.save(item);
    }
}
