package com.workspace.api.event.service;

import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;
import com.workspace.api.schedule.tasks.repository.TasksRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {
    private final EventService events;
    private final TasksRepository tasks;
    private final CurrentUser user;

    public record Entry(Object id, String title, LocalDate date, String startTime, String endTime,
                        String description, String location, EventSource source, EventCategory category,
                        boolean readOnly) {
    }

    public List<Entry> list() {
        List<Entry> result = new ArrayList<>();
        events.findAll().forEach(event -> result.add(new Entry(event.id(), event.title(), event.date(),
                event.startTime().toString(), event.endTime(), event.description() == null ? "" : event.description(), event.location(),
                event.source(), event.category(), false)));

        tasks.findAllByUserId(user.id()).stream()
                .filter(task -> task.getDueDate() != null && !Boolean.TRUE.equals(task.getCompleted()))
                .forEach(task -> {
                    LocalTime start = task.getDueTime() == null ? LocalTime.of(8, 0) : task.getDueTime();
                    int endMinutes = Math.min(1440, start.getHour() * 60 + start.getMinute() + 60);
                    String end = endMinutes == 1440 ? "24:00" : LocalTime.of(endMinutes / 60, endMinutes % 60).toString();
                    result.add(new Entry("task-" + task.getId(), task.getTitle(), task.getDueDate(), start.toString(), end,
                            task.getNotes() == null ? "" : task.getNotes(), null, EventSource.internal, EventCategory.personal, true));
                });

        result.sort(Comparator.comparing(Entry::date).thenComparing(Entry::startTime));
        return result;
    }
}
