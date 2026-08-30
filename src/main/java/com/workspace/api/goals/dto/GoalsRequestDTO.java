package com.workspace.api.goals.dto;

import com.workspace.api.habit.entity.Category;
import com.workspace.api.util.domain.Priority;

public record GoalsRequestDTO(
        String name,
        String expectedTarget,
        Priority priority,
        Category category,
        Integer habitId
) {
}
