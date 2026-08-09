package com.workspace.api.task.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskResponseDTO (
        Integer id,
        String name,
        LocalDate date,
        LocalTime executionTime,
        LocalTime timeEstimated,
        @JsonProperty("priority") Priority taskPriority,
        String note,
        Boolean completed
){
    public TaskResponseDTO(Task task){
        this(task.getId(), task.getName(), task.getDate(), task.getExecutionTime(), task.getTimeEstimated(), task.getTaskPriority(), task.getNote(), task.getCompleted());
    }
}