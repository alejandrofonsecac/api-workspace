package com.workspace.api.planner.service;

import com.workspace.api.event.domain.*;
import com.workspace.api.event.dto.*;
import com.workspace.api.event.repository.EventRepository;
import com.workspace.api.event.service.EventService;
import com.workspace.api.planner.dto.*;
import com.workspace.api.schedule.tasks.repository.TasksRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlannerService {
    private final EventService events;
    private final EventRepository eventRepository;
    private final TasksRepository tasks;
    private final CurrentUser currentUser;
    private final Clock clock;

    public record PlannerTask(Long id, String title, String category) {
    }

    public List<PlannerTask> unscheduled() {
        return tasks.findAllByUserId(currentUser.id()).stream().filter(task -> !Boolean.TRUE.equals(task.getCompleted()))
                .map(task -> new PlannerTask(task.getId(), task.getTitle(), "personal")).toList();
    }

    @Transactional
    public EventResponseDTO schedule(Long taskId, ScheduleTaskRequest request) {
        events.lockCalendar();
        var existing = eventRepository.findByScheduledTaskIdAndUserId(taskId, currentUser.id());
        if (existing.isPresent()) {
            Event event = existing.get();
            if (!event.getDate().equals(request.date()) || !event.getStartTime().equals(request.startTime()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already scheduled at another time");
            return new EventResponseDTO(event);
        }
        var task = tasks.findByIdAndUserId(taskId, currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (Boolean.TRUE.equals(task.getCompleted()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed tasks cannot be scheduled");
        if (request.startTime().getMinute() != 0 || request.startTime().getSecond() != 0 || request.startTime().getNano() != 0)
            throw new com.workspace.api.planner.exception.InvalidPlannerSlotException("Tasks use a full-hour block");
        var created = events.create(new EventRequestDTO(task.getTitle(), request.date(), request.startTime(),
                request.startTime().plusHours(1), task.getNotes(), null, EventSource.internal, EventCategory.personal));
        Event event = eventRepository.findByIdAndUserId(created.id(), currentUser.id()).orElseThrow();
        event.setKind("task");
        event.setScheduledTaskId(taskId);
        event.setRecurring(task.isRecurring());
        event.setRecurrenceRule(task.getRecurrenceRule());
        tasks.delete(task);
        return new EventResponseDTO(eventRepository.saveAndFlush(event));
    }

    public record DaySummary(LocalDate date, List<EventResponseDTO> events, PlannerAvailabilityResponseDTO availability,
                             long taskCount, long eventCount, int plannedMinutes, int freeMinutes,
                             int freeTimePercentage,
                             EventResponseDTO nextActivity) {
    }

    public DaySummary summary(LocalDate date) {
        var dayEvents = eventRepository.findByUserIdAndDateOrderByStartTimeAsc(currentUser.id(), date).stream().map(EventResponseDTO::new).toList();
        var availability = events.getAvailability(date);
        var today = LocalDate.now(clock);
        var next = dayEvents.stream().filter(event -> !date.isBefore(today)
                && (!date.equals(today) || !event.startTime().isBefore(LocalTime.now(clock).withSecond(0).withNano(0)))).findFirst().orElse(null);
        return new DaySummary(date, dayEvents, availability,
                unscheduled().size() + dayEvents.stream().filter(event -> "task".equals(event.kind())).count(),
                dayEvents.stream().filter(event -> "event".equals(event.kind())).count(),
                availability.occupiedBlocks() * 60, availability.availableBlocks() * 60,
                (int) Math.round(availability.availableBlocks() * 100.0 / availability.totalBlocks()), next);
    }
}

