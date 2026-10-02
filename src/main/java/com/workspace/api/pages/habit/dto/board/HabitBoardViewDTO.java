package com.workspace.api.pages.habit.dto.board;

import com.workspace.api.pages.habit.service.HabitMetrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record HabitBoardViewDTO(
        Integer id,
        String name,
        String icon,
        String categoryId,
        String frequencyType,
        String time,
        String goal,
        String note,
        String status,
        List<LocalDate> completedDates,
        LocalDate createdAt,
        Set<Integer> daysOfWeek,
        LocalDate asOfDate,
        HabitMetrics.Metrics metrics,
        List<HabitMetrics.HistoryDay> history
) {
}
