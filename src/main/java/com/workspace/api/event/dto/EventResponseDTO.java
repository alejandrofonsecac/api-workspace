package com.workspace.api.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.event.domain.Event;
import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventResponseDTO(
        Long id,
        String userId,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String description,
        String location,
        EventSource source,
        EventCategory category
) {
    public EventResponseDTO(Event event) {
        this(
                event.getId(),
                event.getUserId(),
                event.getTitle(),
                event.getDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDescription(),
                event.getLocation(),
                event.getSource(),
                event.getCategory()
        );
    }
}
