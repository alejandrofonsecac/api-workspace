package com.workspace.api.schedule.tasks.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TasksDTO(
        Long id,
        String userId,
        String title,
        String priority,
        String dueLabel,
        Boolean completed,
        String notes,
        LocalDate dueDate,
        @JsonFormat(pattern="HH:mm") LocalTime dueTime,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Instant completedAt,
        Boolean isRecurring,
        String recurrenceRule
) {
}
