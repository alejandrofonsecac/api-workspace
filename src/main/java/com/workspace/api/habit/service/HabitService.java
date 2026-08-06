package com.workspace.api.habit.service;

import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HabitService {
    private HabitRepository habitRepository;

    public HabitResponseDTO createHabit(HabitResponseDTO habitDTO){
        Habit habit = new Habit();
        habit.setId(habitDTO.id());
        habit.setName(habitDTO.name());
        habit.setCategory(habitDTO.category());
        habit.setFrequencyEveryDay(habitDTO.frequencyEveryDay());
        habit.setLocalDate(habitDTO.localDate());
        habit.setObjective(habitDTO.objective());
        habit.setObservation(habitDTO.observation());

        Habit habitSaved = habitRepository.save(habit);

        return new HabitResponseDTO(
                habitSaved.getId(),
                habitSaved.getName(),
                habitSaved.getCategory(),
                habitSaved.getFrequencyEveryDay(),
                habitSaved.getLocalDate(),
                habitSaved.getObjective(),
                habitSaved.getObservation()
        );
    }
}
