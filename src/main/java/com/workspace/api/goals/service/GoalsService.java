package com.workspace.api.goals.service;

import com.workspace.api.goals.dto.GoalsResponseDTO;
import com.workspace.api.goals.entity.Goals;
import com.workspace.api.goals.repository.GoalsRepository;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalsService {
    private final GoalsRepository goalsRepository;
    private final HabitRepository habitRepository;

    @Transactional(readOnly = true)
    public List<GoalsResponseDTO> getAllGoals(){
        return goalsRepository.findAll().stream()
                .map(GoalsResponseDTO::new)
                .toList();
    }

    @Transactional
    public GoalsResponseDTO createGoals(GoalsResponseDTO dto){
        Goals goal = new Goals();
        goal.setGoalName(dto.name());
        goal.setExpectedTarget(dto.expectedTarget());
        goal.setPriority(dto.priority());
        goal.setCategory(dto.category());
        Habit habit = habitRepository.findById(dto.habitId())
                .orElseThrow(() -> new IllegalArgumentException("Habito nao encontrado: " + dto.habitId()));
        goal.setHabit(habit);

        Goals saveGoal = goalsRepository.save(goal);

        return new GoalsResponseDTO(saveGoal);
    }
}
