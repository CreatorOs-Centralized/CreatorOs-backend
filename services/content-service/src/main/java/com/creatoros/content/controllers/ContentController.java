package com.creatoros.content.controllers;

import com.creatoros.content.dtos.request.CreateContentRequest;
import com.creatoros.content.dtos.request.UpdateContentRequest;
import com.creatoros.content.dtos.response.ContentResponse;
import com.creatoros.content.mappers.ContentMapper;
import com.creatoros.content.services.ContentService;
import com.creatoros.content.utils.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ContentResponse create(@Valid @RequestBody CreateContentRequest request) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return ContentMapper.toResponse(
                contentService.createContent(userId, request.getTitle(), request.getContentType())
        );
    }

    @GetMapping
    public List<ContentResponse> myContents() {
        UUID userId = UserContextUtil.getCurrentUserId();
        return contentService.getMyContents(userId).stream()
                .map(ContentMapper::toResponse)
                .toList();
    }

    @PutMapping("/{contentId}")
    public ContentResponse update(
            @PathVariable UUID contentId,
            @Valid @RequestBody UpdateContentRequest request
    ) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return ContentMapper.toResponse(
                contentService.updateContent(userId, contentId, request)
        );
    }

    @DeleteMapping("/{contentId}")
    public void delete(@PathVariable UUID contentId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        contentService.deleteContent(userId, contentId);
    }
}
