package com.creatoros.scheduler.services;

import com.creatoros.scheduler.entities.ScheduledJob;
import com.creatoros.scheduler.models.ScheduleRequest;
import com.creatoros.scheduler.repositories.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduledJobRepository repository;

    public void createSchedule(String userId, ScheduleRequest request) {

        ScheduledJob job = ScheduledJob.builder()
                .userId(UUID.fromString(userId))
                .contentItemId(request.getContentItemId())
                .connectedAccountId(request.getConnectedAccountId())
                .platform(request.getPlatform())
                .scheduledAt(request.getScheduledAt())
                .status("PENDING")
                .retryCount(0)
                .build();

        repository.save(job);
    }
}
