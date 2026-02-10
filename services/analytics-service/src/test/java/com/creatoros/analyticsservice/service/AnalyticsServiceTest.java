package com.creatoros.analyticsservice.service;

import com.creatoros.analyticsservice.model.CreatorAnalyticsSummary;
import com.creatoros.analyticsservice.model.PostMetrics;
import com.creatoros.analyticsservice.repository.CreatorAnalyticsSummaryRepository;
import com.creatoros.analyticsservice.repository.PostMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private PostMetricsRepository postMetricsRepository;

    @Mock
    private CreatorAnalyticsSummaryRepository creatorAnalyticsSummaryRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void savePostMetrics_ShouldReturnSavedMetrics() {
        PostMetrics metrics = PostMetrics.builder().userId(UUID.randomUUID()).views(100L).build();
        when(postMetricsRepository.save(any(PostMetrics.class))).thenReturn(metrics);

        PostMetrics saved = analyticsService.savePostMetrics(metrics);

        assertNotNull(saved);
        assertEquals(100L, saved.getViews());
        verify(postMetricsRepository, times(1)).save(metrics);
    }

    @Test
    void getMetricsByUserId_ShouldReturnList_WhenNoPlatform() {
        UUID userId = UUID.randomUUID();
        when(postMetricsRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<PostMetrics> result = analyticsService.getMetricsByUserId(userId, null);

        assertNotNull(result);
        verify(postMetricsRepository, times(1)).findByUserId(userId);
        verify(postMetricsRepository, never()).findByUserIdAndPlatform(any(), any());
    }

    @Test
    void getMetricsByUserId_ShouldFilterByPlatform_WhenPlatformProvided() {
        UUID userId = UUID.randomUUID();
        String platform = "INSTAGRAM";
        when(postMetricsRepository.findByUserIdAndPlatform(userId, platform)).thenReturn(Collections.emptyList());

        List<PostMetrics> result = analyticsService.getMetricsByUserId(userId, platform);

        assertNotNull(result);
        verify(postMetricsRepository, times(1)).findByUserIdAndPlatform(userId, platform);
        verify(postMetricsRepository, never()).findByUserId(any());
    }

    @Test
    void saveAnalyticsSummary_ShouldReturnSavedSummary() {
        CreatorAnalyticsSummary summary = CreatorAnalyticsSummary.builder().userId(UUID.randomUUID()).totalViews(5000L).build();
        when(creatorAnalyticsSummaryRepository.save(any(CreatorAnalyticsSummary.class))).thenReturn(summary);

        CreatorAnalyticsSummary saved = analyticsService.saveAnalyticsSummary(summary);

        assertNotNull(saved);
        assertEquals(5000L, saved.getTotalViews());
        verify(creatorAnalyticsSummaryRepository, times(1)).save(summary);
    }

    @Test
    void getAnalyticsSummary_ShouldUseDefaults_WhenDatesMissing() {
        UUID userId = UUID.randomUUID();
        // Mocking the repo call with any dates because the service sets defaults
        when(creatorAnalyticsSummaryRepository.findByUserIdAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<CreatorAnalyticsSummary> result = analyticsService.getAnalyticsSummary(userId, null, null, null);

        assertNotNull(result);
        verify(creatorAnalyticsSummaryRepository).findByUserIdAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
                eq(userId), any(LocalDate.class), any(LocalDate.class));
    }
    
    @Test
    void getAnalyticsSummary_ShouldFilterByPlatform_WhenProvided() {
        UUID userId = UUID.randomUUID();
        String platform = "YOUTUBE";
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        
        when(creatorAnalyticsSummaryRepository.findByUserIdAndPlatformAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
                userId, platform, start, end))
                .thenReturn(Collections.emptyList());

        List<CreatorAnalyticsSummary> result = analyticsService.getAnalyticsSummary(userId, platform, start, end);

        assertNotNull(result);
        verify(creatorAnalyticsSummaryRepository).findByUserIdAndPlatformAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
                userId, platform, start, end);
    }
}
