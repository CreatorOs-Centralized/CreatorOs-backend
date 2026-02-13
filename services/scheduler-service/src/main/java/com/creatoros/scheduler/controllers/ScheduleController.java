package com.creatoros.scheduler.controllers;

import com.creatoros.scheduler.models.ScheduleRequest;
import com.creatoros.scheduler.services.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/schedule")
    public String schedule(@RequestBody ScheduleRequest request) {
        scheduleService.createSchedule(request);
        return "Scheduled successfully";
    }
}
