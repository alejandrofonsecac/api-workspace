package com.workspace.api.tasks.dto;

import com.workspace.api.tasks.domain.Tasks;

public record TasksResponseDTO(
        Long id,
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes
) {
    public TasksResponseDTO(Tasks task) {
        this(
                task.getId(),
                task.getTitle(),
                task.getPriority(),
                task.getDueLabel(),
                task.getCompleted(),
                task.getNotes()
        );
    }
}
