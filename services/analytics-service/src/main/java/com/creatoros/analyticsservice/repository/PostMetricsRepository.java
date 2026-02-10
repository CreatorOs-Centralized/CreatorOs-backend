package com.creatoros.analyticsservice.repository;

import com.creatoros.analyticsservice.model.PostMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMetricsRepository extends JpaRepository<PostMetrics, UUID> {
    List<PostMetrics> findByUserId(UUID userId);

    List<PostMetrics> findByUserIdAndPlatform(UUID userId, String platform);

    List<PostMetrics> findByPublishedPostId(UUID publishedPostId);
}
