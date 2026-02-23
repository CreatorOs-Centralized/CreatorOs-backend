package com.creatoros.publishing.controllers;

import com.creatoros.publishing.entities.PublishedPost;
import com.creatoros.publishing.repositories.PublishedPostRepository;
import com.creatoros.publishing.utils.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/published-posts")
@RequiredArgsConstructor
public class PublishedPostController {

    private final PublishedPostRepository publishedPostRepository;

    /**
     * Get all published posts for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<PublishedPost>> getAllPosts() {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<PublishedPost> posts = publishedPostRepository.findAllByUserId(userId);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get published post by ID
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PublishedPost> getPostById(
            @PathVariable UUID postId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        Optional<PublishedPost> post = publishedPostRepository.findByIdAndUserId(postId, userId);
        return post.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get posts by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<PublishedPost>> getPostsByPlatform(
            @PathVariable String platform) {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<PublishedPost> posts = publishedPostRepository.findByPlatformAndUserId(platform, userId);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by connected account ID
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<PublishedPost>> getPostsByAccount(
            @PathVariable UUID accountId) {
        UUID userId = UserContextUtil.getCurrentUserId();
        List<PublishedPost> posts = publishedPostRepository.findByConnectedAccountIdAndUserId(accountId, userId);
        return ResponseEntity.ok(posts);
    }
}
