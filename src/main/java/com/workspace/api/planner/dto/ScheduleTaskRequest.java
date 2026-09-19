package com.workspace.api.planner.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
public record ScheduleTaskRequest(@NotNull LocalDate date, @NotNull @JsonFormat(pattern="HH:mm") LocalTime startTime) {}

