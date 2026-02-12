package com.creatoros.publishing.controllers;

import com.creatoros.publishing.entities.PublishedPost;
import com.creatoros.publishing.repositories.PublishedPostRepository;
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
     * Get all published posts
     */
    @GetMapping
    public ResponseEntity<List<PublishedPost>> getAllPosts() {
        List<PublishedPost> posts = publishedPostRepository.findAll();
        return ResponseEntity.ok(posts);
    }

    /**
     * Get published post by ID
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PublishedPost> getPostById(@PathVariable UUID postId) {
        Optional<PublishedPost> post = publishedPostRepository.findById(postId);
        return post.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get posts by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<PublishedPost>> getPostsByPlatform(@PathVariable String platform) {
        List<PublishedPost> posts = publishedPostRepository.findByPlatform(platform);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by connected account ID
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<PublishedPost>> getPostsByAccount(@PathVariable UUID accountId) {
        List<PublishedPost> posts = publishedPostRepository.findByConnectedAccountId(accountId);
        return ResponseEntity.ok(posts);
    }
}
