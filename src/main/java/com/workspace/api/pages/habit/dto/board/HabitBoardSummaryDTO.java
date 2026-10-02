package com.workspace.api.pages.habit.dto.board;

public record HabitBoardSummaryDTO(
        long activeCount,
        int highestCurrentStreak,
        int accumulatedDays,
        int averageConsistency
) {
}
