package com.workspace.api.habit.dto.board;

public record HabitBoardSummaryDTO(
        long activeCount,
        int highestCurrentStreak,
        int accumulatedDays,
        int averageConsistency
) {
}
