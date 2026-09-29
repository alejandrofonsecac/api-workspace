package com.workspace.api.habit.service;

import com.workspace.api.habit.entity.Habit;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class HabitMetrics {
    public record Metrics(int currentStreak, int bestStreak, int consistency, int completedDays, Integer milestone) {
    }

    public boolean scheduled(Habit habit, LocalDate date) {
        if (habit.getCreatedAt() != null && date.isBefore(habit.getCreatedAt())) return false;
        return switch (habit.getFrequencyType()) {
            case "weekdays" -> date.getDayOfWeek().getValue() <= 5;
            case "custom" -> habit.getDaysOfWeek().contains(date.getDayOfWeek().getValue());
            default -> true;
        };
    }

    public record HistoryDay(LocalDate date, boolean completed) {
    }

    public List<HistoryDay> history(Habit habit, LocalDate today) {
        var history = new ArrayList<HistoryDay>();
        // Thirty weekly occurrences fit in 210 calendar days. Bound the scan even
        // for legacy custom schedules with no selected weekday.
        var earliest = today.minusDays(209);
        if (habit.getCreatedAt() != null && habit.getCreatedAt().isAfter(earliest))
            earliest = habit.getCreatedAt();
        for (var date = today; !date.isBefore(earliest) && history.size() < 30; date = date.minusDays(1)) {
            if (scheduled(habit, date))
                history.add(new HistoryDay(date, habit.getCompletedDates().contains(date)));
        }
        Collections.reverse(history);
        return List.copyOf(history);
    }

    public Metrics calculate(Habit habit, LocalDate today) {
        var dates = habit.getCompletedDates().stream().filter(date -> !date.isAfter(today) && scheduled(habit, date)).sorted().toList();
        int best = 0, run = 0;
        LocalDate previous = null;
        for (var date : dates) {
            boolean consecutive = previous != null;
            if (previous != null) for (var day = previous.plusDays(1); day.isBefore(date); day = day.plusDays(1)) {
                if (scheduled(habit, day)) {
                    consecutive = false;
                    break;
                }
            }
            run = consecutive ? run + 1 : 1;
            best = Math.max(best, run);
            previous = date;
        }
        int current = 0;
        if (!dates.isEmpty()) {
            var day = habit.getCompletedDates().contains(today) ? today : today.minusDays(1);
            var first = dates.getFirst();
            for (; !day.isBefore(first); day = day.minusDays(1)) {
                if (!scheduled(habit, day)) continue;
                if (!habit.getCompletedDates().contains(day)) break;
                current++;
            }
        }
        int expected = 0, completed = 0;
        for (var day = today.minusDays(29); !day.isAfter(today); day = day.plusDays(1)) {
            if (!scheduled(habit, day)) continue;
            expected++;
            if (habit.getCompletedDates().contains(day)) completed++;
        }
        Integer milestone = null;
        for (int threshold : new int[]{365, 90, 30, 7})
            if (best >= threshold) {
                milestone = threshold;
                break;
            }
        return new Metrics(current, best, expected == 0 ? 0 : (int) Math.round(completed * 100.0 / expected), dates.size(), milestone);
    }
}

