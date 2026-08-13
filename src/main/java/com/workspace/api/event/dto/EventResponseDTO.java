package com.workspace.api.event.dto;

import com.workspace.api.event.domain.Event;
import com.workspace.api.util.domain.Category;

import java.time.LocalTime;

public record EventResponseDTO (Integer id, String name, String description, LocalTime startTime, LocalTime endTime, Category category) {
    public EventResponseDTO (Event event) {
        this(event.getId(), event.getName(), event.getDescription(), event.getStartTime(), event.getEndTime(), event.getCategory());
    }
}
