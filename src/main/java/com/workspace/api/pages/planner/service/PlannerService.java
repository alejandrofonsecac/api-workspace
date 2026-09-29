package com.workspace.api.pages.planner.service;

import com.workspace.api.pages.event.dto.EventResponseDTO;
import com.workspace.api.pages.event.repository.EventRepository;
import com.workspace.api.pages.event.service.EventService;
import com.workspace.api.pages.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.dto.*;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlannerService {
    private final EventService events;
    private final EventRepository eventRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public record DaySummary(LocalDate date, List<EventResponseDTO> events, PlannerAvailabilityResponseDTO availability,
                             long undatedCount, long eventCount, int plannedMinutes, int freeMinutes,
                             int freeTimePercentage,
                             EventResponseDTO nextActivity) {
    }

    public DaySummary summary(LocalDate date) {
        var dayEvents = eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), date).stream()
                .map(EventResponseDTO::new).toList();
        var availability = events.getAvailability(date);
        var today = LocalDate.now(clock);
        var next = dayEvents.stream()
                .filter(event ->
                    event.startTime() != null)
                .filter(event ->
                        !Boolean.TRUE.equals(event.completed()))
                .filter(event ->
                        !date.isBefore(today) && (!date.equals(today) || !event.startTime().isBefore(LocalTime.now(clock).withSecond(0).withNano(0))))
                .findFirst()
                .orElse(null);

        return new DaySummary(date, dayEvents, availability,
                eventRepository.findAllByUserId(currentUser.id()).stream()
                        .filter(event ->
                                event.getDate() == null)
                        .count(),
                dayEvents.size(), availability.occupiedBlocks() * 60, availability.availableBlocks() * 60,
                (int) Math.round(availability.availableBlocks() * 100.0 / availability.totalBlocks()), next);
    }
}

