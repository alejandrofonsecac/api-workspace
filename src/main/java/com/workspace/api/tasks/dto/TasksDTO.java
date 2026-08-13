package com.workspace.api.tasks.dto;

import com.workspace.api.tasks.domain.Tasks;

import java.time.LocalDate;

public record TasksDTO(
        Long id,
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes
) {
    public TasksDTO(Tasks tasks) {
        this(
                tasks.getId(),
                tasks.getTitle(),
                tasks.getPriority(),
                tasks.getDueLabel(),
                tasks.getCompleted(),
                tasks.getNotes()
        );
    }
}
