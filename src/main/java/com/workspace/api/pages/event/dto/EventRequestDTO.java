package com.workspace.api.pages.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.pages.event.domain.EventCategory;
import com.workspace.api.pages.event.domain.EventSource;
import com.workspace.api.pages.event.domain.EventTimes;
import com.workspace.api.pages.event.validation.ValidEventTimeRange;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

@ValidEventTimeRange
public record EventRequestDTO(
        @NotBlank @Size(max = 255)
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        String endTime,
        String description,
        String location,
        @NotNull EventSource source,
        @NotNull EventCategory category,
        Boolean completed,
        Boolean isRecurring,
        String recurrenceRule,
        @Pattern(regexp = "Alta|Média|Normal") String priority
) {

    public EventRequestDTO(String title,
                           LocalDate date,
                           LocalTime startTime,
                           String endTime,
                           String description,
                           String location,
                           EventSource source,
                           EventCategory category
    ) {
        this(title, date, startTime, endTime, description, location, source, category, null, null, null, null);
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
