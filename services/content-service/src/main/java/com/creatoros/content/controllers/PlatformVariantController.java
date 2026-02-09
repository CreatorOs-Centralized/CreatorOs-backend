package com.creatoros.content.controllers;

import com.creatoros.content.dtos.request.CreatePlatformVariantRequest;
import com.creatoros.content.dtos.response.PlatformVariantResponse;
import com.creatoros.content.entities.ContentItem;
import com.creatoros.content.entities.ContentPlatformVariant;
import com.creatoros.content.exceptions.ResourceNotFoundException;
import com.creatoros.content.repositories.ContentItemRepository;
import com.creatoros.content.repositories.ContentPlatformVariantRepository;
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
@RequestMapping("/contents/{contentId}/variants")
@RequiredArgsConstructor
public class PlatformVariantController {

    private final ContentItemRepository contentItemRepository;
    private final ContentPlatformVariantRepository variantRepository;

    @PostMapping
    public PlatformVariantResponse createOrUpdate(
            @PathVariable UUID contentId,
            @Valid @RequestBody CreatePlatformVariantRequest request
    ) {
        UUID userId = UserContextUtil.getCurrentUserId();
        
        ContentItem item = contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));

        ContentPlatformVariant variant = ContentPlatformVariant.builder()
                .contentItem(item)
                .platform(request.getPlatform())
                .variantType(request.getVariantType())
                .value(request.getValue())
                .build();

        ContentPlatformVariant saved = variantRepository.save(variant);

        return PlatformVariantResponse.builder()
                .id(saved.getId())
                .platform(saved.getPlatform())
                .variantType(saved.getVariantType())
                .value(saved.getValue())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @GetMapping
    public List<PlatformVariantResponse> list(@PathVariable UUID contentId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        
        contentItemRepository.findByIdAndUserId(contentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));

        return variantRepository.findByContentItemId(contentId)
                .stream()
                .map(v -> PlatformVariantResponse.builder()
                        .id(v.getId())
                        .platform(v.getPlatform())
                        .variantType(v.getVariantType())
                        .value(v.getValue())
                        .createdAt(v.getCreatedAt())
                        .updatedAt(v.getUpdatedAt())
                        .build())
                .toList();
    }
}
