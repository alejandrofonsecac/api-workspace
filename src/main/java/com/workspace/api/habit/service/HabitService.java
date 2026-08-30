package com.workspace.api.habit.service;

import com.workspace.api.habit.dto.HabitRequestDTO;
import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class HabitService {
    private HabitRepository habitRepository;

    public HabitResponseDTO createHabit(HabitRequestDTO request){
        Habit habit = new Habit();
        habit.setName(request.name());
        habit.setCategory(request.category());
        habit.setFrequencyEveryDay(request.frequencyEveryDay());
        habit.setLocalDate(request.localDate());
        habit.setObjective(request.objective());
        habit.setObservation(request.observation());

        Habit habitSaved = habitRepository.save(habit);

        return new HabitResponseDTO(habitSaved);
    }

    public List<HabitResponseDTO> findAllHabits(){
        return habitRepository.findAll().stream()
                .map(HabitResponseDTO::new)
                .toList();
    }
}
