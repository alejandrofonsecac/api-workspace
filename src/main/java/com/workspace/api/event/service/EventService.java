package com.workspace.api.event.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.event.domain.*;
import com.workspace.api.event.dto.*;
import com.workspace.api.event.repository.EventRepository;
import com.workspace.api.planner.dto.*;
import com.workspace.api.planner.exception.*;
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
        validate(request, null);
        Event event = new Event();
        event.setUserId(currentUser.id());
        apply(event, request);
        return new EventResponseDTO(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO request) {
        lockCalendar();
        Event event = find(id);
        validate(request, id);
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
        List<Event> events = eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), date);
        List<PlannerSlotDTO> slots = IntStream.range(0, 24).mapToObj(hour -> {
            boolean occupied = events.stream().anyMatch(event -> EventTimes.startMinutes(event.getStartTime()) < (hour + 1) * 60
                    && EventTimes.endMinutes(event.getEndTime()) > hour * 60);
            return new PlannerSlotDTO(LocalTime.of(hour, 0), LocalTime.of((hour + 1) % 24, 0), !occupied);
        }).toList();
        int occupied = (int) slots.stream().filter(slot -> !slot.available()).count();
        return new PlannerAvailabilityResponseDTO(date, 24, occupied, 24 - occupied, 24 - occupied, slots);
    }

    public long countEvents() { return eventRepository.countByUserIdAndDate(currentUser.id(), LocalDate.now(clock)); }

    private Event find(Long id) {
        return eventRepository.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void validate(EventRequestDTO request, Long ignoredId) {
        if (!EventTimes.valid(request.startTime(), request.endTime())) throw new InvalidPlannerSlotException("Use an increasing interval between 00:00 and 24:00.");
        int start = EventTimes.startMinutes(request.startTime());
        int end = EventTimes.endMinutes(EventTimes.parseEnd(request.endTime()));
        boolean occupied = eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), request.date()).stream()
                .anyMatch(event -> !Objects.equals(event.getId(), ignoredId)
                        && EventTimes.startMinutes(event.getStartTime()) < end && EventTimes.endMinutes(event.getEndTime()) > start);
        if (occupied) throw new OccupiedPlannerSlotException("This time slot is already occupied for the selected date.");
    }

    private void apply(Event event, EventRequestDTO request) {
        event.setTitle(request.title().trim()); event.setDate(request.date());
        event.setStartTime(request.startTime()); event.setEndTime(EventTimes.parseEnd(request.endTime()));
        event.setDescription(request.description()); event.setLocation(request.location());
        event.setSource(request.source()); event.setCategory(request.category());
    }
}
