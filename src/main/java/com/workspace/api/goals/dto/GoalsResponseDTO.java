package com.workspace.api.goals.dto;

import com.workspace.api.goals.entity.Goals;
import com.workspace.api.habit.entity.Category;
import com.workspace.api.task.entity.Priority;

public record GoalsResponseDTO (Integer id, String name, String expectedTarget, Priority priority, Category category, Integer habitId){
    public GoalsResponseDTO(Goals saveGoal) {
        this(saveGoal.getId(), saveGoal.getGoalName(), saveGoal.getExpectedTarget(), saveGoal.getPriority(), saveGoal.getCategory(), saveGoal.getHabit().getId());
    }
}
