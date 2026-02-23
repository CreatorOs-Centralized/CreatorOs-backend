package com.creatoros.publishing.controllers;

import com.creatoros.publishing.services.LinkedInPostService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/linkedin/posts")
@RequiredArgsConstructor
@Slf4j
public class LinkedInPostController {

    private final LinkedInPostService linkedinPostService;

    /**
     * Get all posts for a connected LinkedIn account
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getUserPosts(
            @PathVariable UUID accountId) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            log.info("Fetching posts for account: {}", accountId);
            Map<String, Object> posts = linkedinPostService.getUserPosts(userId, accountId);
            return ResponseEntity.ok(posts);
        } catch (RuntimeException ex) {
            log.error("Error fetching posts: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch posts: " + ex.getMessage()));
        }
    }

    /**
     * Publish a new post to LinkedIn
     */
    @PostMapping("/{accountId}")
    public ResponseEntity<?> publishPost(
            @PathVariable UUID accountId,
            @RequestBody Map<String, String> request
    ) {
        try {
            UUID userId = UserContextUtil.getCurrentUserId();
            String postText = request.get("text");
            if (postText == null || postText.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Post text is required"));
            }

            log.info("Publishing post for account: {}", accountId);
            Map<String, Object> result = linkedinPostService.publishPost(userId, accountId, postText);
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            log.error("Error publishing post: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to publish post: " + ex.getMessage()));
        }
    }
}
