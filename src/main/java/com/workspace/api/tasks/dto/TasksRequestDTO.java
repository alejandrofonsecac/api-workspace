package com.workspace.api.tasks.dto;

public record TasksRequestDTO(
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes
) {
}
