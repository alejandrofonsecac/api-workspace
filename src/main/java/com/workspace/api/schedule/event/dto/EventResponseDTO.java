package com.workspace.api.schedule.event.dto;

import com.workspace.api.schedule.event.domain.Event;
import com.workspace.api.util.domain.Category;

import java.time.LocalTime;

public record EventResponseDTO (Integer id, String userId, String name, String description, LocalTime startTime, LocalTime endTime, Category category) {
    public EventResponseDTO (Event event) {
        this(event.getId(), event.getUserId(), event.getName(), event.getDescription(), event.getStartTime(), event.getEndTime(), event.getCategory());
    }
}
