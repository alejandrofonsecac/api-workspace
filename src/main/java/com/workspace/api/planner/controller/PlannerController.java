package com.workspace.api.planner.controller;

import com.workspace.api.event.service.EventService;
import com.workspace.api.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.dto.ScheduleTaskRequest;
import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.planner.service.PlannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@RestController
@RequestMapping("/planner")
@RequiredArgsConstructor
public class PlannerController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final EventService eventService;
    private final com.workspace.api.planner.service.PlannerService planner;

    @GetMapping("/summary")
    public PlannerService.DaySummary summary(@RequestParam LocalDate date) {
        return planner.summary(date);
    }

    @GetMapping("/tasks")
    public List<PlannerService.PlannerTask> tasks() { return planner.unscheduled(); }

    @PostMapping("/tasks/{id}/schedule")
    public com.workspace.api.event.dto.EventResponseDTO schedule(@PathVariable Long id,
            @Valid @RequestBody ScheduleTaskRequest request) {
        return planner.schedule(id, request);
    }

    @GetMapping("/availability")
    public PlannerAvailabilityResponseDTO getAvailability(@RequestParam String date) {
        try {
            return eventService.getAvailability(LocalDate.parse(date, DATE_FORMATTER));
        } catch (DateTimeParseException exception) {
            throw new InvalidPlannerSlotException("date must use the YYYY-MM-DD format.");
        }
    }
}
