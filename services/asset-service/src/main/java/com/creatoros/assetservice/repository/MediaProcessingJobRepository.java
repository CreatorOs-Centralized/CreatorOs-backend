package com.creatoros.assetservice.repository;

import com.creatoros.assetservice.model.MediaProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaProcessingJobRepository extends JpaRepository<MediaProcessingJob, UUID> {
    List<MediaProcessingJob> findByStatus(MediaProcessingJob.JobStatus status);
}
