package com.creatoros.analyticsservice.service;

import com.creatoros.analyticsservice.model.PostMetrics;
import com.creatoros.analyticsservice.repository.PostMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PostMetricsRepository postMetricsRepository;
    private final com.creatoros.analyticsservice.repository.CreatorAnalyticsSummaryRepository creatorAnalyticsSummaryRepository;

    @Transactional
    public PostMetrics savePostMetrics(PostMetrics postMetrics) {
        return postMetricsRepository.save(postMetrics);
    }

    @Transactional
    public com.creatoros.analyticsservice.model.CreatorAnalyticsSummary saveAnalyticsSummary(com.creatoros.analyticsservice.model.CreatorAnalyticsSummary summary) {
        return creatorAnalyticsSummaryRepository.save(summary);
    }

    public List<PostMetrics> getMetricsByUserId(UUID userId, String platform) {
        if (platform != null && !platform.isEmpty()) {
            return postMetricsRepository.findByUserIdAndPlatform(userId, platform);
        }
        return postMetricsRepository.findByUserId(userId);
    }

    public List<PostMetrics> getMetricsByPostId(UUID postId) {
        return postMetricsRepository.findByPublishedPostId(postId);
    }
    
    public List<com.creatoros.analyticsservice.model.CreatorAnalyticsSummary> getAnalyticsSummary(UUID userId, String platform, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Default to last 30 days if dates are missing
        if (startDate == null) startDate = java.time.LocalDate.now().minusDays(30);
        if (endDate == null) endDate = java.time.LocalDate.now();

        if (platform != null && !platform.isEmpty()) {
            return creatorAnalyticsSummaryRepository.findByUserIdAndPlatformAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(userId, platform, startDate, endDate);
        }
        return creatorAnalyticsSummaryRepository.findByUserIdAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(userId, startDate, endDate);
    }
}
