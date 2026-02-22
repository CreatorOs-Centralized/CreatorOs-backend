package com.creatoros.analyticsservice.repository;

import com.creatoros.analyticsservice.model.Insights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InsightsRepository extends JpaRepository<Insights, UUID> {
    List<Insights> findByUserId(UUID userId);
}
