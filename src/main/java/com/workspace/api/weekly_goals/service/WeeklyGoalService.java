package com.workspace.api.weekly_goals.service;

import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import com.workspace.api.weekly_goals.dto.WeeklyGoalDTO;
import com.workspace.api.weekly_goals.entity.WeeklyGoal;
import com.workspace.api.weekly_goals.repository.WeeklyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyGoalService {

    private final WeeklyGoalRepository weeklyGoalRepository;
    private final HabitRepository habitRepository;

    @Transactional(readOnly = true)
    public List<WeeklyGoalDTO> findAll() {
        return weeklyGoalRepository.findAll().stream()
                .map(WeeklyGoalDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyGoalDTO findById(Integer id) {
        return new WeeklyGoalDTO(findEntityById(id));
    }

    @Transactional
    public WeeklyGoalDTO create(WeeklyGoalDTO dto) {
        validateRequiredFields(dto);

        WeeklyGoal weeklyGoal = new WeeklyGoal();
        applyDto(weeklyGoal, dto);
        return new WeeklyGoalDTO(weeklyGoalRepository.save(weeklyGoal));
    }

    @Transactional
    public WeeklyGoalDTO update(Integer id, WeeklyGoalDTO dto) {
        validateRequiredFields(dto);

        WeeklyGoal weeklyGoal = findEntityById(id);
        applyDto(weeklyGoal, dto);
        return new WeeklyGoalDTO(weeklyGoalRepository.save(weeklyGoal));
    }

    @Transactional
    public void delete(Integer id) {
        weeklyGoalRepository.delete(findEntityById(id));
    }

    private void applyDto(WeeklyGoal weeklyGoal, WeeklyGoalDTO dto) {
        weeklyGoal.setTitle(dto.title().trim());
        weeklyGoal.setDescription(dto.description().trim());
        weeklyGoal.setPriority(dto.priority());
        weeklyGoal.setCategory(dto.category());
        weeklyGoal.setHabit(findHabitById(dto.habitId()));
    }

    private WeeklyGoal findEntityById(Integer id) {
        return weeklyGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Weekly goal not found: " + id));
    }

    private Habit findHabitById(Integer habitId) {
        return habitRepository.findById(habitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found: " + habitId));
    }

    private void validateRequiredFields(WeeklyGoalDTO dto) {
        if (dto == null
                || dto.title() == null || dto.title().isBlank()
                || dto.description() == null || dto.description().isBlank()
                || dto.priority() == null
                || dto.category() == null
                || dto.habitId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "title, description, priority, category and habitId are required"
            );
        }
    }
}
