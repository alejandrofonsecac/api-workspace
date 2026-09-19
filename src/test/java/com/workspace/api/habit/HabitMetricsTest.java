package com.workspace.api.habit;

import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.service.HabitMetrics;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class HabitMetricsTest {
    private final HabitMetrics metrics = new HabitMetrics();
    @Test void weekendsDoNotBreakAWeekdayStreakAndTodayCanStillBeCompleted() {
        var habit = new Habit(); habit.setFrequencyType("weekdays"); habit.setCreatedAt(LocalDate.parse("2026-09-10"));
        habit.setCompletedDates(Set.of(LocalDate.parse("2026-09-10"),LocalDate.parse("2026-09-11"),LocalDate.parse("2026-09-14")));
        var result=metrics.calculate(habit,LocalDate.parse("2026-09-15"));
        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.bestStreak()).isEqualTo(3);
        assertThat(result.consistency()).isEqualTo(75);
        assertThat(metrics.calculate(habit,LocalDate.parse("2026-09-16")).currentStreak()).isZero();
    }
    @Test void customScheduleIgnoresUnscheduledAndFutureCompletions() {
        var habit=new Habit(); habit.setFrequencyType("custom"); habit.setDaysOfWeek(Set.of(1));
        habit.setCreatedAt(LocalDate.parse("2026-09-01"));
        habit.setCompletedDates(Set.of(LocalDate.parse("2026-09-07"),LocalDate.parse("2026-09-08"),LocalDate.parse("2026-09-14"),LocalDate.parse("2026-09-21")));
        var result=metrics.calculate(habit,LocalDate.parse("2026-09-14"));
        assertThat(result.completedDays()).isEqualTo(2);
        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.consistency()).isEqualTo(100);
    }
}
