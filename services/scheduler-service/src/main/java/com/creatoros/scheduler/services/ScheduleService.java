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

    public void createSchedule(ScheduleRequest request) {

        ScheduledJob job = ScheduledJob.builder()
                .userId(mockUser())
                .contentItemId(request.getContentItemId())
                .connectedAccountId(request.getConnectedAccountId())
                .platform(request.getPlatform())
                .scheduledAt(request.getScheduledAt())
                .status("PENDING")
                .retryCount(0)
                .build();

        repository.save(job);
    }

    private UUID mockUser() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
