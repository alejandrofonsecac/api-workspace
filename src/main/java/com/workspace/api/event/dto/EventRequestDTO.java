package com.workspace.api.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.event.domain.*;
import com.workspace.api.event.validation.ValidEventTimeRange;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@ValidEventTimeRange
public record EventRequestDTO(
        @NotBlank @Size(max = 255)
        String title,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        @NotNull @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @NotBlank
        String endTime,
        String description,
        String location,
        @NotNull EventSource source,
        @NotNull EventCategory category,
        Boolean completed,
        Boolean isRecurring,
        String recurrenceRule
) {

    public EventRequestDTO(String title, LocalDate date, LocalTime startTime, String endTime,
                           String description, String location, EventSource source, EventCategory category) {
        this(title, date, startTime, endTime, description, location, source, category, null, null, null);
    }

    public EventRequestDTO(String title,
                           LocalDate date,
                           LocalTime startTime,
                           LocalTime endTime,
                           String description,
                           String location,
                           EventSource source,
                           EventCategory category) {
        this(title, date, startTime, endTime == null ? null : EventTimes.endText(endTime), description, location, source, category);
    }
}
