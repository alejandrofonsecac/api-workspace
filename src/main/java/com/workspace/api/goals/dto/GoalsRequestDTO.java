package com.workspace.api.goals.dto;

import com.workspace.api.habit.entity.Category;
import com.workspace.api.util.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GoalsRequestDTO(
        @NotBlank(message = "name is required") @Size(max = 255) String name,
        @NotBlank(message = "expectedTarget is required") @Size(max = 255) String expectedTarget,
        @NotNull(message = "priority is required") Priority priority,
        @NotNull(message = "category is required") Category category,
        Integer habitId,
        @Pattern(regexp = "\\d{4}-W(0[1-9]|[1-4][0-9]|5[0-3])", message = "weekId must use YYYY-Www") String weekId
) {
}
