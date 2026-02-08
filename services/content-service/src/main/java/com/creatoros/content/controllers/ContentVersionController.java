package com.creatoros.content.controllers;

import com.creatoros.content.dtos.request.ContentVersionRequest;
import com.creatoros.content.dtos.response.ContentVersionResponse;
import com.creatoros.content.mappers.ContentMapper;
import com.creatoros.content.services.ContentVersionService;
import com.creatoros.content.utils.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contents/{contentId}/versions")
@RequiredArgsConstructor
public class ContentVersionController {

    private final ContentVersionService contentVersionService;

    @PostMapping
    public ContentVersionResponse create(
            @PathVariable UUID contentId,
            @Valid @RequestBody ContentVersionRequest request
    ) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return ContentMapper.toVersionResponse(
                contentVersionService.createVersion(userId, contentId, request.getBody())
        );
    }

    @GetMapping
    public List<ContentVersionResponse> list(@PathVariable UUID contentId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        return contentVersionService.getVersions(userId, contentId).stream()
                .map(ContentMapper::toVersionResponse)
                .toList();
    }
}
