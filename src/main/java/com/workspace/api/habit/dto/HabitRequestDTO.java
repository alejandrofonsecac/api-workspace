package com.workspace.api.habit.dto;

import com.workspace.api.util.domain.Category;

import java.time.LocalDate;

public record HabitRequestDTO(
        String name,
        Category category,
        Boolean frequencyEveryDay,
        LocalDate localDate,
        String objective,
        String observation
) {
}
