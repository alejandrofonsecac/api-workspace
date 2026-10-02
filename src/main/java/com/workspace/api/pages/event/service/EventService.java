package com.workspace.api.pages.event.service;

import com.workspace.api.pages.event.domain.Event;
import com.workspace.api.pages.event.domain.EventTimes;
import com.workspace.api.pages.event.dto.EventRequestDTO;
import com.workspace.api.pages.event.dto.EventResponseDTO;
import com.workspace.api.pages.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.pages.planner.dto.PlannerSlotDTO;
import com.workspace.api.pages.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.pages.planner.exception.OccupiedPlannerSlotException;
import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.recurrence.RecurrencePolicy;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.pages.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final CurrentUser currentUser;
    private final UserRepository users;
    private final Clock clock;

    public List<EventResponseDTO> findAll() {
        return eventRepository.findByUserIdOrderByDateAscStartTimeAsc(currentUser.id()).stream().map(EventResponseDTO::new).toList();
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO request) {
        lockCalendar();
        var dates = RecurrencePolicy.dates(request.date(), request.isRecurring(), request.recurrenceRule());
        var existing = existingByDate(dates);
        // Validate every date under the user lock before creating any occurrence.
        for (var date : dates){
            validate(onDate(request, date, false), null, existing.getOrDefault(date, List.of()));
        }

        Event first = null;
        for (var date : dates) {
            Event event = new Event();
            event.setUserId(currentUser.id());
            apply(event, onDate(request, date, first == null && Boolean.TRUE.equals(request.completed())));
            eventRepository.save(event);
            if (first == null) first = event;
        }
        eventRepository.flush();
        return new EventResponseDTO(first);
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO request) {
        lockCalendar();
        Event event = find(id);
        RecurrencePolicy.dates(request.date(), request.isRecurring(), request.recurrenceRule());
        validate(request, id, request.date() == null ? List.of()
                : eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), request.date()));
        apply(event, request);
        return new EventResponseDTO(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public void delete(Long id) {
        lockCalendar();
        eventRepository.delete(find(id));
    }

    // Serializes writes for this user's calendar, including concurrent empty-slot creations.
    public void lockCalendar() {
        users.lockById(currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public PlannerAvailabilityResponseDTO getAvailability(LocalDate date) {
        return getAvailability(date, eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), date));
    }

    public PlannerAvailabilityResponseDTO getAvailability(LocalDate date, List<Event> events) {
        List<PlannerSlotDTO> slots = IntStream.range(0, 24).mapToObj(hour -> {
            boolean occupied = events.stream()
                    .filter(event ->
                            event.getStartTime() != null && event.getEndTime() != null
                    ).anyMatch(event ->
                            EventTimes.startMinutes(event.getStartTime()) < (hour + 1) * 60
                    && EventTimes.endMinutes(event.getEndTime()) > hour * 60);

            return new PlannerSlotDTO(LocalTime.of(hour, 0), LocalTime.of((hour + 1) % 24, 0), !occupied);
        }).toList();

        int occupied = (int) slots.stream()
                .filter(slot
                        -> !slot.available()).count();
        return new PlannerAvailabilityResponseDTO(date, 24, occupied, 24 - occupied, 24 - occupied, slots);
    }

    public long countEvents() {
        return eventRepository.countByUserIdAndDate(currentUser.id(), LocalDate.now(clock));
    }

    private Event find(Long id) {
        return eventRepository.findForUpdateByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    // One query for the whole recurrence window instead of one per occurrence.
    private Map<LocalDate, List<Event>> existingByDate(List<LocalDate> dates) {
        if (dates.isEmpty() || dates.getFirst() == null) return Collections.emptyMap();
        return eventRepository.findByUserIdAndDateBetweenOrderByStartTimeAsc(
                        currentUser.id(), dates.getFirst(), dates.getLast()).stream()
                .collect(java.util.stream.Collectors.groupingBy(Event::getDate));
    }

    private void validate(EventRequestDTO request, Long ignoredId, List<Event> sameDayEvents) {
        if ((request.date() == null && request.startTime() != null) || !EventTimes.valid(request.startTime(), request.endTime())){
            throw new InvalidPlannerSlotException("Use an increasing interval between 00:00 and 24:00.");
        }

        if (request.startTime() == null || request.endTime() == null) return;

        int start = EventTimes.startMinutes(request.startTime());
        int end = EventTimes.endMinutes(EventTimes.parseEnd(request.endTime()));

        boolean occupied = sameDayEvents.stream()
                .filter(event ->
                        event.getStartTime() != null && event.getEndTime() != null)
                .anyMatch(event ->
                        !Objects.equals(event.getId(), ignoredId)
                        && EventTimes.startMinutes(event.getStartTime()) < end && EventTimes.endMinutes(event.getEndTime()) > start);
        if (occupied){
            throw new OccupiedPlannerSlotException("This time slot is already occupied for the selected date.");
        }
    }

    private EventRequestDTO onDate(EventRequestDTO request, LocalDate date, boolean completed) {
        return new EventRequestDTO(request.title(), date, request.startTime(), request.endTime(),
                request.description(), request.location(), request.source(), request.category(),
                completed, request.isRecurring(), request.recurrenceRule(), request.priority());
    }

    private void apply(Event event, EventRequestDTO request) {
        event.setPriority(request.priority());
        event.setTitle(request.title().trim());
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setEndTime(EventTimes.parseEnd(request.endTime()));
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setSource(request.source());
        event.setCategory(request.category());
        event.updateRetention(request.completed(), request.isRecurring(), request.recurrenceRule(), clock.instant());
    }
}
