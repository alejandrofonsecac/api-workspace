package com.workspace.api.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;
import com.workspace.api.event.validation.ValidEventTimeRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@ValidEventTimeRange
public record EventRequestDTO(
        @NotBlank(message = "title must not be blank") String title,
        @NotNull(message = "date is required")
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @NotNull(message = "startTime is required")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull(message = "endTime is required")
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String description,
        String location,
        @NotNull(message = "source is required") EventSource source,
        @NotNull(message = "category is required") EventCategory category
) {
}
