package com.workspace.api.habit.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record HabitDraftDTO(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 80) String icon,
                            @NotBlank String categoryId,
                            @NotBlank @Pattern(regexp = "daily|weekdays|custom") String frequencyType,
                            String time, @Size(max = 255) String goal, @Size(max = 255) String note,
                            Set<Integer> daysOfWeek) {
}

