package com.creatoros.scheduler.controllers;

import com.creatoros.scheduler.models.ScheduleRequest;
import com.creatoros.scheduler.services.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/schedule")
    public String schedule(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ScheduleRequest request) {
        scheduleService.createSchedule(userId, request);
        return "Scheduled successfully";
    }
}
