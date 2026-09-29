package com.workspace.api.pages.habit.service;

import com.workspace.api.pages.habit.dto.board.HabitBoardDTO;
import com.workspace.api.pages.habit.dto.board.HabitBoardSummaryDTO;
import com.workspace.api.pages.habit.dto.board.HabitBoardViewDTO;
import com.workspace.api.pages.habit.dto.HabitDraftDTO;
import com.workspace.api.pages.habit.entity.Habit;
import com.workspace.api.pages.habit.repository.HabitRepository;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitBoardService {
    private final HabitRepository habits;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final HabitMetrics metrics;

    public HabitBoardDTO board() {
        var views = habits.findAllByUserId(currentUser.id()).stream().map(this::view).toList();
        return new HabitBoardDTO(views, new HabitBoardSummaryDTO(
                views.stream()
                        .filter(v ->
                                "active".equals(v.status())).count(),
                views.stream().mapToInt(v ->
                        v.metrics().currentStreak()).max().orElse(0),
                views.stream().mapToInt(v ->
                        v.metrics().completedDays()).sum(),
                (int) Math.round(views.stream().mapToInt(v ->
                        v.metrics().consistency()).average().orElse(0))
        ));
    }

    public HabitBoardViewDTO view(Habit habit) {
        var today = LocalDate.now(clock);
        var category = habit.getCategory() == null ? Category.PERSONAL : habit.getCategory();

        return new HabitBoardViewDTO(
                habit.getId(), habit.getName(), habit.getIcon(), category.getId(), habit.getFrequencyType(),
                habit.getTime() == null ? null : habit.getTime().toString(), habit.getObjective(), habit.getObservation(), habit.getStatus(),
                habit.getCompletedDates().stream().sorted().toList(), habit.getCreatedAt(), Set.copyOf(habit.getDaysOfWeek()), today, metrics.calculate(habit, today), metrics.history(habit, today)
        );
    }

    @Transactional
    public HabitBoardViewDTO create(HabitDraftDTO draft) {
        var habit = new Habit();
        habit.setUserId(currentUser.id());
        habit.setCreatedAt(LocalDate.now(clock));
        apply(habit, draft);
        return view(habits.saveAndFlush(habit));
    }

    @Transactional
    public HabitBoardViewDTO update(Integer id, HabitDraftDTO draft) {
        var habit = find(id);
        apply(habit, draft);
        return view(habit);
    }

    @Transactional
    public HabitBoardViewDTO status(Integer id, String status) {
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
