package com.workspace.api.schedule.tasks.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TasksDTO(
        Long id,
        String userId,
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes,
        java.time.LocalDate dueDate,
        @JsonFormat(pattern="HH:mm") java.time.LocalTime dueTime
) {
}
