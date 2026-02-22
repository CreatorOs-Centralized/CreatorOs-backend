package com.creatoros.scheduler.services;

import com.creatoros.scheduler.entities.ScheduledJob;
import com.creatoros.scheduler.kafka.PublishEventProducer;
import com.creatoros.scheduler.repositories.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobProcessorService {

    private final ScheduledJobRepository repository;
    private final PublishEventProducer producer;

    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void processDueJobs() {

        log.info("Processing due jobs at {}", LocalDateTime.now());

        List<ScheduledJob> jobs =
                repository.findByStatusAndScheduledAtBefore(
                        "PENDING",
                        LocalDateTime.now()
                );

        log.info("Found {} jobs to process", jobs.size());

        for (ScheduledJob job : jobs) {

            try {
                log.info("Processing job {} for content {} on platform {}", 
                        job.getId(), job.getContentItemId(), job.getPlatform());

                producer.sendPublishRequested(job);

                job.setStatus("TRIGGERED");
                repository.save(job);

                log.info("Successfully triggered job {}", job.getId());

            } catch (Exception ex) {
                log.error("Failed to process job {}: {}", job.getId(), ex.getMessage());
                
                job.setRetryCount(job.getRetryCount() + 1);
                
                // Mark as FAILED if max retries exceeded
                if (job.getRetryCount() >= 3) {
                    job.setStatus("FAILED");
                }
                
                repository.save(job);
            }
        }
    }
}
