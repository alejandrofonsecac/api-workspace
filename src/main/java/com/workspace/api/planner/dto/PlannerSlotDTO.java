package com.workspace.api.planner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record PlannerSlotDTO(
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        boolean available
) {
}
