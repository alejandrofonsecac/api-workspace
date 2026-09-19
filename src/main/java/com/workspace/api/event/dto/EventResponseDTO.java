package com.workspace.api.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.workspace.api.event.domain.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventResponseDTO(Long id, String userId, String title,
                               @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                               @JsonFormat(pattern = "HH:mm") LocalTime startTime, String endTime,
                               String description, String location, EventSource source, EventCategory category,
                               String kind) {
    public EventResponseDTO(Event event) {
        this(event.getId(), event.getUserId(), event.getTitle(), event.getDate(), event.getStartTime(),
                EventTimes.endText(event.getEndTime()), event.getDescription(), event.getLocation(), event.getSource(), event.getCategory(), event.getKind());
    }

    public EventResponseDTO(Long id, String userId, String title, LocalDate date, LocalTime startTime,
                            LocalTime endTime, String description, String location, EventSource source, EventCategory category) {
        this(id, userId, title, date, startTime, EventTimes.endText(endTime), description, location, source, category, "event");
    }
}
