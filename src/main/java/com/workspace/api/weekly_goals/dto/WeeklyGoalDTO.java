package com.workspace.api.weekly_goals.dto;

import com.workspace.api.util.domain.Category;
import com.workspace.api.util.domain.Priority;
import com.workspace.api.weekly_goals.entity.WeeklyGoal;

public record WeeklyGoalDTO(
        Integer id,
        String title,
        String description,
        Priority priority,
        Category category,
        Integer habitId
) {
    public WeeklyGoalDTO(WeeklyGoal weeklyGoal) {
        this(
                weeklyGoal.getId(),
                weeklyGoal.getTitle(),
                weeklyGoal.getDescription(),
                weeklyGoal.getPriority(),
                weeklyGoal.getCategory(),
                weeklyGoal.getHabit().getId()
        );
    }
}
