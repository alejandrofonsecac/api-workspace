package com.workspace.api.planner.controller;

import com.workspace.api.event.service.EventService;
import com.workspace.api.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.planner.service.PlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

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

    @GetMapping("/availability")
    public PlannerAvailabilityResponseDTO getAvailability(@RequestParam String date) {
        try {
            return eventService.getAvailability(LocalDate.parse(date, DATE_FORMATTER));
        } catch (DateTimeParseException exception) {
            throw new InvalidPlannerSlotException("date must use the YYYY-MM-DD format.");
        }
    }
}
