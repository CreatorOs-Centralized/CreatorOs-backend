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

    private final com.creatoros.analyticsservice.repository.PostMetricsDailyRepository postMetricsDailyRepository;

    @Transactional
    public PostMetrics savePostMetrics(PostMetrics postMetrics) {
        return postMetricsRepository.save(postMetrics);
    }

    @Transactional
    public com.creatoros.analyticsservice.model.CreatorAnalyticsSummary saveAnalyticsSummary(
            com.creatoros.analyticsservice.model.CreatorAnalyticsSummary summary) {
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

    public List<com.creatoros.analyticsservice.model.CreatorAnalyticsSummary> getAnalyticsSummary(UUID userId,
            String platform, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Default to last 30 days if dates are missing
        if (startDate == null)
            startDate = java.time.LocalDate.now().minusDays(30);
        if (endDate == null)
            endDate = java.time.LocalDate.now();

        if (platform != null && !platform.isEmpty()) {
            return creatorAnalyticsSummaryRepository
                    .findByUserIdAndPlatformAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(userId, platform,
                            startDate, endDate);
        }
        return creatorAnalyticsSummaryRepository
                .findByUserIdAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(userId, startDate, endDate);
    }

    public com.creatoros.analyticsservice.dto.DashboardSummaryDTO getDashboardSummary(UUID userId, String platform,
            UUID postId) {
        List<PostMetrics> metrics;
        if (postId != null) {
            metrics = postMetricsRepository.findByPublishedPostId(postId);
        } else if (platform != null && !platform.isEmpty()) {
            metrics = postMetricsRepository.findByUserIdAndPlatform(userId, platform);
        } else {
            metrics = postMetricsRepository.findByUserId(userId);
        }

        long views = 0, likes = 0, comments = 0, shares = 0;
        for (PostMetrics m : metrics) {
            views += m.getViews() != null ? m.getViews() : 0;
            likes += m.getLikes() != null ? m.getLikes() : 0;
            comments += m.getComments() != null ? m.getComments() : 0;
            shares += m.getShares() != null ? m.getShares() : 0;
        }

        return com.creatoros.analyticsservice.dto.DashboardSummaryDTO.builder()
                .views(views).likes(likes).comments(comments).shares(shares).build();
    }

    public List<com.creatoros.analyticsservice.dto.TrendDataDTO> getViewsTrend(UUID userId, String platform,
            UUID postId, int days) {
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays(days);

        List<com.creatoros.analyticsservice.model.PostMetricsDaily> dailyMetrics;
        if (postId != null) {
            dailyMetrics = postMetricsDailyRepository.findByPublishedPostIdAndMetricDateBetween(postId, startDate,
                    endDate);
        } else if (platform != null && !platform.isEmpty()) {
            dailyMetrics = postMetricsDailyRepository.findByUserIdAndPlatformAndMetricDateBetween(userId, platform,
                    startDate, endDate);
        } else {
            dailyMetrics = postMetricsDailyRepository.findByUserIdAndMetricDateBetween(userId, startDate, endDate);
        }

        java.util.Map<java.time.LocalDate, com.creatoros.analyticsservice.dto.TrendDataDTO> trendMap = new java.util.TreeMap<>();
        for (int i = 0; i <= days; i++) {
            java.time.LocalDate d = startDate.plusDays(i);
            trendMap.put(d, com.creatoros.analyticsservice.dto.TrendDataDTO.builder().date(d).views(0L).likes(0L)
                    .comments(0L).shares(0L).build());
        }

        for (var m : dailyMetrics) {
            var dto = trendMap.get(m.getMetricDate());
            if (dto != null) {
                dto.setViews(dto.getViews() + (m.getViews() != null ? m.getViews() : 0));
                dto.setLikes(dto.getLikes() + (m.getLikes() != null ? m.getLikes() : 0));
                dto.setComments(dto.getComments() + (m.getComments() != null ? m.getComments() : 0));
                dto.setShares(dto.getShares() + (m.getShares() != null ? m.getShares() : 0));
            }
        }

        return new java.util.ArrayList<>(trendMap.values());
    }

    public List<com.creatoros.analyticsservice.dto.PlatformComparisonDTO> getPlatformComparison(UUID userId, int days) {
        List<PostMetrics> metrics = postMetricsRepository.findByUserId(userId);

        java.util.Map<String, com.creatoros.analyticsservice.dto.PlatformComparisonDTO> compMap = new java.util.HashMap<>();

        for (PostMetrics m : metrics) {
            String p = m.getPlatform();
            if (p == null)
                continue;
            compMap.putIfAbsent(p, com.creatoros.analyticsservice.dto.PlatformComparisonDTO.builder().platform(p)
                    .views(0L).likes(0L).comments(0L).shares(0L).build());
            var dto = compMap.get(p);
            dto.setViews(dto.getViews() + (m.getViews() != null ? m.getViews() : 0));
            dto.setLikes(dto.getLikes() + (m.getLikes() != null ? m.getLikes() : 0));
            dto.setComments(dto.getComments() + (m.getComments() != null ? m.getComments() : 0));
            dto.setShares(dto.getShares() + (m.getShares() != null ? m.getShares() : 0));
        }

        return new java.util.ArrayList<>(compMap.values());
    }

    public List<com.creatoros.analyticsservice.dto.PostOptionDTO> getAvailablePosts(UUID userId, String platform) {
        List<PostMetrics> metrics;
        if (platform != null && !platform.isEmpty()) {
            metrics = postMetricsRepository.findByUserIdAndPlatform(userId, platform);
        } else {
            metrics = postMetricsRepository.findByUserId(userId);
        }

        return metrics.stream().map(m -> com.creatoros.analyticsservice.dto.PostOptionDTO.builder()
                .postId(m.getPublishedPostId())
                .platformPostId(m.getPlatformPostId())
                .title(m.getPostTitle() != null ? m.getPostTitle()
                        : (m.getPlatform() + " Post " + m.getPublishedPostId()))
                .platform(m.getPlatform())
                .build()).toList();
    }
}
