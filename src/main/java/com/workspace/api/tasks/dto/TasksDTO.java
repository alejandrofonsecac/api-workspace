package com.workspace.api.tasks.dto;

import com.workspace.api.tasks.domain.Tasks;

public record TasksDTO(
        Long id,
        String title,
        String priority,
        String dueLabel,
        String period,
        String project,
        Boolean completed,
        String notes,
        String assignee) {
    public TasksDTO(Tasks tasks) {
        this(tasks.getId(),
                tasks.getTitle(),
                tasks.getPriority(),
                tasks.getDueLabel(),
                tasks.getPeriod(),
                tasks.getProject(),
                tasks.getCompleted(),
                tasks.getNotes(),
                tasks.getAssignee());
    }
}
