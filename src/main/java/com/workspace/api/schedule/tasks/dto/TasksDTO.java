package com.workspace.api.schedule.tasks.dto;

import com.workspace.api.schedule.tasks.domain.Tasks;

public record TasksDTO(
        Long id,
        String userId,
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes
) {
    public TasksDTO(Tasks tasks) {
        this(
                tasks.getId(),
                tasks.getUserId(),
                tasks.getTitle(),
                tasks.getPriority(),
                tasks.getDueLabel(),
                tasks.getCompleted(),
                tasks.getNotes()
        );
    }
}
