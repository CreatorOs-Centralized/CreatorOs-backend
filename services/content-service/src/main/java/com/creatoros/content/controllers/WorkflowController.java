package com.creatoros.content.controllers;

import com.creatoros.content.dtos.response.ContentResponse;
import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.mappers.ContentMapper;
import com.creatoros.content.services.WorkflowService;
import com.creatoros.content.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/contents/{contentId}/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/next")
    public ContentResponse moveToNext(@PathVariable UUID contentId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        ContentItem item = workflowService.moveToNextState(contentId, userId);
        return ContentMapper.toResponse(item);
    }
}
