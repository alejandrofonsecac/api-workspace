package com.workspace.api.habit.dto;

import com.workspace.api.util.domain.Category;
import com.workspace.api.habit.entity.Habit;

import java.time.LocalDate;
import java.util.List;

public record HabitResponseDTO (
        Integer id,
        String userId,
        String name,
        Category category,
        Boolean frequencyEveryDay,
        LocalDate localDate,
        String objective,
        String observation,
        List<LocalDate> completedDates) {

    public HabitResponseDTO(Habit habit) {
        this(
                habit.getId(),
                habit.getUserId(),
                habit.getName(),
                habit.getCategory(),
                habit.getFrequencyEveryDay(),
                habit.getLocalDate(),
                habit.getObjective(),
                habit.getObservation(),
                habit.getCompletedDates().stream().sorted().toList()
        );
    }
}
