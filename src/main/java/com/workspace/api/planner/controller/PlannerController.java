package com.workspace.api.planner.controller;

import com.workspace.api.event.service.EventService;
import com.workspace.api.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/availability")
    public PlannerAvailabilityResponseDTO getAvailability(@RequestParam String date) {
        try {
            return eventService.getAvailability(LocalDate.parse(date, DATE_FORMATTER));
        } catch (DateTimeParseException exception) {
            throw new InvalidPlannerSlotException("date must use the YYYY-MM-DD format.");
        }
    }
}
