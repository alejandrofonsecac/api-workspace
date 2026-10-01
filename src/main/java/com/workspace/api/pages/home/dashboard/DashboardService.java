package com.workspace.api.pages.home.dashboard;

import com.workspace.api.pages.habit.service.HabitBoardService;
import com.workspace.api.pages.habit.service.HabitMetrics;
import com.workspace.api.pages.planner.service.PlannerService;
import com.workspace.api.pages.habit.repository.HabitRepository;
import com.workspace.api.pages.event.domain.Event;
import com.workspace.api.pages.event.repository.EventRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final PlannerService planner;
    private final EventRepository events;
    private final HabitRepository habits;
    private final HabitMetrics metrics;
    private final HabitBoardService habitBoard;
    private final CurrentUser user;
    private final Clock clock;

    public record Focus(Long id, String title, String description, int progress) {
    }

    public record DailyHabit(Integer id, String icon, String name, int completed, int target, int progress) {
    }

    public record Summary(long completedEvents, long totalEvents, int highestCurrentStreak) {
    }

    public record Dashboard(LocalDate date, PlannerService.DaySummary planner, Focus focus, List<DailyHabit> habits,
                            Summary summary) {
    }

    public Dashboard get() {
        var today = LocalDate.now(clock);
        var dayEvents = events.findByUserIdAndDateOrderByStartTimeAsc(user.id(), today).stream().sorted(java.util.Comparator.comparing(Event::getId)).toList();
        var focus = dayEvents.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())).findFirst()
                .map(t -> new Focus(t.getId(), t.getTitle(), t.getDescription(), 0)).orElse(null);
        var dailyHabits = habits.findAllByUserId(user.id()).stream()
                .filter(h -> "active".equals(h.getStatus()) && metrics.scheduled(h, today)).map(h -> {
                    boolean completed = h.getCompletedDates().contains(today);
                    return new DailyHabit(h.getId(), h.getIcon(), h.getName(), completed ? 1 : 0, 1, completed ? 100 : 0);
                }).toList();
        return new Dashboard(today, planner.summary(today), focus, dailyHabits,
                new Summary(dayEvents.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count(), dayEvents.size(), habitBoard.board().summary().highestCurrentStreak()));
    }
}

