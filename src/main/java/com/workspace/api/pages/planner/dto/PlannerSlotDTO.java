package com.workspace.api.pages.planner.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import com.workspace.api.pages.event.domain.EventTimes;
public record PlannerSlotDTO(@JsonFormat(pattern="HH:mm") LocalTime startTime, String endTime, boolean available) {
    public PlannerSlotDTO(LocalTime startTime, LocalTime endTime, boolean available) {
        this(startTime, EventTimes.endText(endTime), available);
    }
}
