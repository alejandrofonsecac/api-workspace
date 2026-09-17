package com.workspace.api.event.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.event.domain.Event;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.dto.PlannerSlotDTO;
import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.planner.exception.OccupiedPlannerSlotException;
import com.workspace.api.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {
        return eventRepository.findByUserIdOrderByDateAscStartTimeAsc(currentUser.id()).stream()
                .map(EventResponseDTO::new)
                .toList();
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO request) {
        validateTimeSlot(request.startTime(), request.endTime());
        ensureTimeSlotIsAvailable(request.date(), request.startTime(), request.endTime(), null);

        Event event = new Event();
        event.setUserId(currentUser.id());
        applyRequest(event, request);
        return new EventResponseDTO(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO request) {
        Event event = findEntityById(id);
        validateTimeSlot(request.startTime(), request.endTime());
        ensureTimeSlotIsAvailable(request.date(), request.startTime(), request.endTime(), id);
        applyRequest(event, request);
        return new EventResponseDTO(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public PlannerAvailabilityResponseDTO getAvailability(LocalDate date) {
        List<Event> events = eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), date);
        List<PlannerSlotDTO> slots = IntStream.range(8, 20)
                .mapToObj(hour -> buildSlot(events, LocalTime.of(hour, 0)))
                .toList();
        int occupiedBlocks = (int) slots.stream().filter(slot -> !slot.available()).count();

        return new PlannerAvailabilityResponseDTO(
                date,
                12,
                occupiedBlocks,
                12 - occupiedBlocks,
                12 - occupiedBlocks,
                slots
        );
    }

    @Transactional
    public void delete(Long id) {
        Event event = findEntityById(id);
        eventRepository.delete(event);
    }

    private Event findEntityById(Long id) {
        return eventRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + id));
    }

    private void applyRequest(Event event, EventRequestDTO request) {
        event.setTitle(request.title());
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setSource(request.source());
        event.setCategory(request.category());
    }

    public long countEvents() {
        return eventRepository.countByUserIdAndDate(currentUser.id(), LocalDate.now());
    }

    private void validateTimeSlot(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null
                || startTime.getMinute() != 0
                || startTime.getSecond() != 0
                || startTime.getNano() != 0
                || !endTime.equals(startTime.plusHours(1))
                || startTime.isBefore(LocalTime.of(8, 0))
                || endTime.isAfter(LocalTime.of(20, 0))) {
            throw new InvalidPlannerSlotException(
                    "Invalid time slot. Use one full-hour block between 08:00 and 20:00, such as 08:00-09:00."
            );
        }
    }

    private void ensureTimeSlotIsAvailable(
            LocalDate date, LocalTime startTime, LocalTime endTime, Long eventId) {
        boolean occupied = eventId == null
                ? eventRepository.existsByUserIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(currentUser.id(), date, endTime, startTime)
                : eventRepository.existsByUserIdAndDateAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        currentUser.id(), date, endTime, startTime, eventId);

        if (occupied) {
            throw new OccupiedPlannerSlotException(
                    "This time slot is already occupied for the selected date."
            );
        }
    }

    private PlannerSlotDTO buildSlot(List<Event> events, LocalTime startTime) {
        LocalTime endTime = startTime.plusHours(1);
        boolean occupied = events.stream().
                anyMatch(event ->
                event.getStartTime()
                        .isBefore(endTime)
                        &&
                        event.getEndTime()
                                .isAfter(startTime));

        return new PlannerSlotDTO(startTime, endTime, !occupied);
    }
}
