package com.creatoros.analyticsservice.repository;

import com.creatoros.analyticsservice.model.CreatorAnalyticsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreatorAnalyticsSummaryRepository extends JpaRepository<CreatorAnalyticsSummary, UUID> {
    List<CreatorAnalyticsSummary> findByUserIdAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
            UUID userId, LocalDate startDate, LocalDate endDate);

    List<CreatorAnalyticsSummary> findByUserIdAndPlatformAndRangeStartGreaterThanEqualAndRangeEndLessThanEqual(
            UUID userId, String platform, LocalDate startDate, LocalDate endDate);
}
