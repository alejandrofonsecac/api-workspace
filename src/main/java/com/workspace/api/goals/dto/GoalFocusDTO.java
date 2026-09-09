package com.workspace.api.goals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoalFocusDTO(
        @NotBlank(message = "focus is required") @Size(max = 255) String focus,
        @Size(max = 2000) String message) {}
