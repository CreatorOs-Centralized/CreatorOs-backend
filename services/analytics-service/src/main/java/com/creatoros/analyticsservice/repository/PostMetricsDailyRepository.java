package com.creatoros.analyticsservice.repository;

import com.creatoros.analyticsservice.model.PostMetricsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostMetricsDailyRepository extends JpaRepository<PostMetricsDaily, UUID> {
    List<PostMetricsDaily> findByUserIdAndMetricDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    List<PostMetricsDaily> findByUserIdAndPlatformAndMetricDateBetween(UUID userId, String platform,
            LocalDate startDate, LocalDate endDate);

    List<PostMetricsDaily> findByPublishedPostIdAndMetricDateBetween(UUID publishedPostId, LocalDate startDate,
            LocalDate endDate);

    List<PostMetricsDaily> findByPublishedPostId(UUID publishedPostId);
}
