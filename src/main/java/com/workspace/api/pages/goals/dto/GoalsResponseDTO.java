package com.workspace.api.pages.goals.dto;

import com.workspace.api.pages.goals.entity.GoalStatus;
import com.workspace.api.util.domain.Category;
import com.workspace.api.util.domain.Priority;
import java.time.Instant;

public record GoalsResponseDTO(
        Integer id, String userId, String name, String expectedTarget, Priority priority, Category category,
        String weekId,
        GoalStatus status, Instant createdAt, Instant completedAt, int deferredCount,
        boolean canComplete, boolean canDefer) {}
