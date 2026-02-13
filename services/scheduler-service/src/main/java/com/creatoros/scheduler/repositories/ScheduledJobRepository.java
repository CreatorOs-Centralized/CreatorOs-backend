package com.creatoros.scheduler.repositories;

import com.creatoros.scheduler.entities.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {

    List<ScheduledJob> findByStatusAndScheduledAtBefore(String status, LocalDateTime scheduledAt);
}
