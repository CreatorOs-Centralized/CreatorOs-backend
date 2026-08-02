package com.creatoros.analyticsservice.controller;

import com.creatoros.analyticsservice.model.CreatorAnalyticsSummary;
import com.creatoros.analyticsservice.model.PostMetrics;
import com.creatoros.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/metrics")
    public ResponseEntity<PostMetrics> createPostMetrics(
            @RequestBody @jakarta.validation.constraints.NotNull @jakarta.validation.Valid PostMetrics postMetrics) {
        postMetrics.setId(null); // Ensure creation new entity
        return ResponseEntity.ok(analyticsService.savePostMetrics(postMetrics));
    }

    @PostMapping("/summary")
    public ResponseEntity<CreatorAnalyticsSummary> createAnalyticsSummary(
            @RequestBody @jakarta.validation.constraints.NotNull CreatorAnalyticsSummary summary) {
        summary.setId(null); // Ensure creation new entity
        return ResponseEntity.ok(analyticsService.saveAnalyticsSummary(summary));
    }

    @GetMapping("/metrics/user/{userId}")
    public ResponseEntity<List<PostMetrics>> getMetricsByUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(analyticsService.getMetricsByUserId(userId, platform));
    }

    @GetMapping("/summary/{userId}")
    public ResponseEntity<List<CreatorAnalyticsSummary>> getAnalyticsSummary(
            @PathVariable UUID userId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(userId, platform, startDate, endDate));
    }

    @GetMapping("/metrics/post/{postId}")
    public ResponseEntity<List<PostMetrics>> getMetricsByPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(analyticsService.getMetricsByPostId(postId));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<com.creatoros.analyticsservice.dto.DashboardSummaryDTO> getDashboardSummary(
            @RequestParam UUID userId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) UUID postId) {
        return ResponseEntity.ok(analyticsService.getDashboardSummary(userId, platform, postId));
    }

    @GetMapping("/dashboard/trend")
    public ResponseEntity<List<com.creatoros.analyticsservice.dto.TrendDataDTO>> getViewsTrend(
            @RequestParam UUID userId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) UUID postId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getViewsTrend(userId, platform, postId, days));
    }

    @GetMapping("/dashboard/comparison")
    public ResponseEntity<List<com.creatoros.analyticsservice.dto.PlatformComparisonDTO>> getPlatformComparison(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getPlatformComparison(userId, days));
    }

    @GetMapping("/dashboard/posts")
    public ResponseEntity<List<com.creatoros.analyticsservice.dto.PostOptionDTO>> getAvailablePosts(
            @RequestParam UUID userId,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(analyticsService.getAvailablePosts(userId, platform));
    }
}
