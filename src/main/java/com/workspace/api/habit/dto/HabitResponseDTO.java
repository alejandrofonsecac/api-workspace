package com.workspace.api.habit.dto;

import com.workspace.api.habit.entity.Category;
import com.workspace.api.habit.entity.Habit;
import java.time.LocalDate;

public record HabitResponseDTO (
        Integer id,
        String name,
        Category category,
        Boolean frequencyEveryDay,
        LocalDate localDate,
        String objective,
        String observation){

    public HabitResponseDTO(Habit habit){
        this(habit.getId(),habit.getName(), habit.getCategory(), habit.getFrequencyEveryDay(), habit.getLocalDate(), habit.getObjective(), habit.getObservation());
    }
}
