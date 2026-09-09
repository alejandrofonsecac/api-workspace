package com.workspace.api.goals.dto;

import com.workspace.api.goals.entity.GoalStatus;
import com.workspace.api.habit.entity.Category;
import com.workspace.api.util.domain.Priority;
import java.time.Instant;

public record GoalsResponseDTO(
        Integer id, String name, String expectedTarget, Priority priority, Category category,
        Integer habitId, String weekId, String habitName,
        GoalStatus status, Instant createdAt, Instant completedAt, int deferredCount,
        boolean canComplete, boolean canDefer) {}
