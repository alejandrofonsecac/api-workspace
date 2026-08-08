package com.workspace.api.habit.dto;

import com.workspace.api.habit.entity.Category;
import java.time.LocalDate;

public record HabitResponseDTO (
        Integer id,
        String name,
        Category category,
        Boolean frequencyEveryDay,
        LocalDate localDate,
        String objective,
        String observation){

}
