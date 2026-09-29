package com.workspace.api.habit.dto.board;

import java.util.List;

public record HabitBoardDTO(
        List<HabitBoardViewDTO> habits,
        HabitBoardSummaryDTO summary
) {
}
