package com.workspace.api.habit.service;

import com.workspace.api.habit.dto.HabitDraftDTO;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.util.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitBoardService {
    private final HabitRepository habits;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final HabitMetrics metrics;

    public record View(Integer id, String name, String icon, String categoryId, String frequencyType, String time,
                       String goal, String note,
                       String status, List<LocalDate> completedDates, LocalDate createdAt, Set<Integer> daysOfWeek,
                       LocalDate asOfDate, HabitMetrics.Metrics metrics) {
    }

    public record Summary(long activeCount, int highestCurrentStreak, int accumulatedDays, int averageConsistency) {
    }

    public record Board(List<View> habits, Summary summary) {
    }

    public Board board() {
        var views = habits.findAllByUserId(currentUser.id()).stream().map(this::view).toList();
        return new Board(views, new Summary(views.stream().filter(v -> "active".equals(v.status())).count(),
                views.stream().mapToInt(v -> v.metrics().currentStreak()).max().orElse(0),
                views.stream().mapToInt(v -> v.metrics().completedDays()).sum(),
                (int) Math.round(views.stream().mapToInt(v -> v.metrics().consistency()).average().orElse(0))));
    }

    public View view(Habit habit) {
        var today = LocalDate.now(clock);
        var category = habit.getCategory() == null ? Category.PERSONAL : habit.getCategory();
        return new View(habit.getId(), habit.getName(), habit.getIcon(), category.getId(), habit.getFrequencyType(),
                habit.getTime() == null ? null : habit.getTime().toString(), habit.getObjective(), habit.getObservation(), habit.getStatus(),
                habit.getCompletedDates().stream().sorted().toList(), habit.getCreatedAt(), Set.copyOf(habit.getDaysOfWeek()), today, metrics.calculate(habit, today));
    }

    @Transactional
    public View create(HabitDraftDTO draft) {
        var habit = new Habit();
        habit.setUserId(currentUser.id());
        habit.setCreatedAt(LocalDate.now(clock));
        apply(habit, draft);
        return view(habits.saveAndFlush(habit));
    }

    @Transactional
    public View update(Integer id, HabitDraftDTO draft) {
        var habit = find(id);
        apply(habit, draft);
        return view(habit);
    }

    @Transactional
    public View status(Integer id, String status) {
        if (!"active".equals(status) && !"paused".equals(status)) {
            throw invalid("Invalid habit status");
        }
        var habit = find(id);
        habit.setStatus(status);
        return view(habit);
    }

    @Transactional
    public void delete(Integer id) {
        habits.delete(find(id));
    }

    private Habit find(Integer id) {
        return habits.findLockedByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));
    }

    private void apply(Habit habit, HabitDraftDTO draft) {
        var category = Category.fromId(draft.categoryId())
                .orElseThrow(() -> invalid("Invalid category"));

        var days = draft.daysOfWeek() == null ? Set.<Integer>of() : draft.daysOfWeek();

        if (days.stream().anyMatch(day -> day == null || day < 1 || day > 7)
                || ("custom".equals(draft.frequencyType()) && days.isEmpty())) {
            throw invalid("Select weekdays from 1 (Monday) to 7 (Sunday)");
        }

        LocalTime time = null;
        if (draft.time() != null && !draft.time().isBlank()) {
            if (!draft.time().matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
                throw invalid("Invalid time");
            }
            time = LocalTime.parse(draft.time());
        }

        habit.setName(draft.name().trim());
        habit.setIcon(draft.icon());
        habit.setCategory(category);
        habit.setFrequencyType(draft.frequencyType());
        habit.setFrequencyEveryDay("daily".equals(draft.frequencyType()));
        habit.getDaysOfWeek().clear();
        habit.getDaysOfWeek().addAll(days);
        habit.setTime(time);
        habit.setObjective(draft.goal());
        habit.setObservation(draft.note());
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
