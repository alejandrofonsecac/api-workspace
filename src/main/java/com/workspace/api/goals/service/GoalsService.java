package com.workspace.api.goals.service;

import com.workspace.api.goals.dto.GoalsRequestDTO;
import com.workspace.api.goals.dto.GoalsResponseDTO;
import com.workspace.api.goals.entity.Goals;
import com.workspace.api.goals.repository.GoalsRepository;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public GoalsResponseDTO getGoalById(Integer id) {
        return new GoalsResponseDTO(findGoalById(id));
    }

    @Transactional
    public GoalsResponseDTO createGoals(GoalsRequestDTO request){
        Goals goal = new Goals();
        applyRequest(goal, request);
        return new GoalsResponseDTO(goalsRepository.save(goal));
    }

    @Transactional
    public GoalsResponseDTO updateGoal(Integer id, GoalsRequestDTO request) {
        Goals goal = findGoalById(id);
        applyRequest(goal, request);
        return new GoalsResponseDTO(goalsRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(Integer id) {
        goalsRepository.delete(findGoalById(id));
    }

    private Goals findGoalById(Integer id) {
        return goalsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found: " + id));
    }

    private void applyRequest(Goals goal, GoalsRequestDTO request) {
        if (request.habitId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "habitId is required");
        }
        Habit habit = habitRepository.findById(request.habitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found: " + request.habitId()));
        goal.setGoalName(request.name());
        goal.setExpectedTarget(request.expectedTarget());
        goal.setPriority(request.priority());
        goal.setCategory(request.category());
        goal.setHabit(habit);
    }
}
