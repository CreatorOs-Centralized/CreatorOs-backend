package com.creatoros.publishing.controllers;

import com.creatoros.publishing.dto.InstagramPublishRequest;
import com.creatoros.publishing.services.InstagramContentService;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/instagram")
@RequiredArgsConstructor
@Slf4j
public class InstagramContentController {

    private final InstagramContentService instagramContentService;

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody InstagramPublishRequest request) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.publish(userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "result", result
            ));
        } catch (RuntimeException ex) {
            log.error("Error publishing Instagram content: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram publish error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to publish Instagram content: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/posts")
    public ResponseEntity<?> getPosts(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "25") Integer limit,
            @RequestParam(required = false) String after) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.getPosts(userId, accountId, limit, after);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram posts: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram posts error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram posts: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/posts/{mediaId}")
    public ResponseEntity<?> getPostById(
            @PathVariable UUID accountId,
            @PathVariable String mediaId) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.getPostById(userId, accountId, mediaId);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram post: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram post detail error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram post: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/posts/{mediaId}/insights")
    public ResponseEntity<?> getMediaInsights(
            @PathVariable UUID accountId,
            @PathVariable String mediaId,
            @RequestParam(required = false) String metrics) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.getMediaInsights(userId, accountId, mediaId, metrics);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram media insights: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram media insights error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram media insights: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/insights")
    public ResponseEntity<?> getAccountInsights(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String metrics,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            LocalDate sinceDate = since != null && !since.isBlank() ? LocalDate.parse(since) : null;
            LocalDate untilDate = until != null && !until.isBlank() ? LocalDate.parse(until) : null;

            Map<String, Object> result = instagramContentService.getAccountInsights(
                    userId,
                    accountId,
                    metrics,
                    period,
                    sinceDate,
                    untilDate
            );

            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram account insights: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram account insights error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram account insights: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/publishing-limit")
    public ResponseEntity<?> getPublishingLimit(@PathVariable UUID accountId) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.getPublishingLimit(userId, accountId);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram publishing limit: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram publishing limit error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram publishing limit: " + ex.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountId}/containers/{containerId}/status")
    public ResponseEntity<?> getContainerStatus(
            @PathVariable UUID accountId,
            @PathVariable String containerId) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            Map<String, Object> result = instagramContentService.getContainerStatus(userId, accountId, containerId);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (RuntimeException ex) {
            log.error("Error fetching Instagram container status: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected Instagram container status error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to fetch Instagram container status: " + ex.getMessage()));
        }
    }
}