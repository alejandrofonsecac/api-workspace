package com.workspace.api.habit.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.habit.dto.HabitRequestDTO;
import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.dto.PlannerHabitDTO;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.repository.HabitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class HabitService {
    private HabitRepository habitRepository;
    private final CurrentUser currentUser;

    @Transactional
    public HabitResponseDTO createHabit(HabitRequestDTO request){
        Habit habit = new Habit();
        habit.setUserId(currentUser.id());
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
        return habitRepository.findAllByUserId(currentUser.id()).stream()
                .map(HabitResponseDTO::new)
                .toList();
    }

    @Transactional
    public HabitResponseDTO toggleCompletion(Integer id, LocalDate date) {
        Habit habit = habitRepository.findLockedByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found: " + id));

        if (!habit.getCompletedDates().remove(date)) {
            habit.getCompletedDates().add(date);
        }

        return new HabitResponseDTO(habit);
    }

    public List<PlannerHabitDTO> plannerHabits(LocalDate date) {
        return habitRepository.findAllByUserId(currentUser.id()).stream()
                .map(habit -> new PlannerHabitDTO(habit.getId(), habit.getUserId(), habit.getName(), habit.getCompletedDates().contains(date)))
                .toList();
    }
}
