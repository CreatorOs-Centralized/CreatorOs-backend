package com.creatoros.publishing.services;

import com.creatoros.publishing.entities.PublishJob;
import com.creatoros.publishing.models.PublishRequestEvent;
import com.creatoros.publishing.models.PublishResult;
import com.creatoros.publishing.repositories.PublishJobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PublishJobService {

    private final PublishJobRepository publishJobRepository;

    public PublishJobService(PublishJobRepository publishJobRepository) {
        this.publishJobRepository = publishJobRepository;
    }

    public PublishJob createPublishJob(PublishJob publishJob) {
        return publishJobRepository.save(publishJob);
    }

    public PublishJob createJob(PublishRequestEvent event) {
        PublishJob job = new PublishJob();
        job.setUserId(event.getUserId());
        job.setConnectedAccountId(event.getConnectedAccountId());
        job.setContentItemId(event.getContentItemId());
        job.setPlatform(event.getPlatform());
        job.setPostType(event.getPostType() == null || event.getPostType().isBlank() ? "POST" : event.getPostType());
        job.setStatus("PENDING");
        job.setScheduledAt(event.getScheduledAt());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return publishJobRepository.save(job);
    }

    public Optional<PublishJob> getPublishJob(UUID jobId) {
        return publishJobRepository.findById(jobId);
    }

    public List<PublishJob> getPublishJobsByUserId(UUID userId) {
        return publishJobRepository.findByUserId(userId);
    }

    public List<PublishJob> getPublishJobsByStatus(String status) {
        return publishJobRepository.findByStatus(status);
    }

    public PublishJob updatePublishJob(PublishJob publishJob) {
        return publishJobRepository.save(publishJob);
    }

    public void markSuccess(PublishJob job, PublishResult result) {
        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus("SUCCESS");
        job.setLastErrorMessage(null);
        job.setUpdatedAt(LocalDateTime.now());
        publishJobRepository.save(job);
    }

    public void markFailure(PublishJob job, String errorMessage) {
        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setFinishedAt(LocalDateTime.now());
        job.setStatus("FAILED");
        job.setLastErrorMessage(errorMessage);
        job.setUpdatedAt(LocalDateTime.now());
        publishJobRepository.save(job);
    }

    public void deletePublishJob(UUID jobId) {
        publishJobRepository.deleteById(jobId);
    }
}
