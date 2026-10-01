package com.workspace.api.pages.habit.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.pages.habit.dto.HabitRequestDTO;
import com.workspace.api.pages.habit.dto.HabitResponseDTO;
import com.workspace.api.pages.habit.dto.PlannerHabitDTO;
import com.workspace.api.pages.habit.entity.Habit;
import com.workspace.api.pages.habit.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitService {
    private final HabitRepository habitRepository;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final HabitMetrics metrics;

    @Transactional
    public HabitResponseDTO createHabit(HabitRequestDTO request) {
        Habit habit = new Habit();
        habit.setUserId(currentUser.id());
        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        habit.setCreatedAt(LocalDate.now(clock));
        if (Boolean.FALSE.equals(request.frequencyEveryDay()) && request.localDate() != null) {
            habit.setFrequencyType("custom");
            habit.getDaysOfWeek().add(request.localDate().getDayOfWeek().getValue());
        }
        habit.setName(request.name());
        habit.setCategory(request.category());
        habit.setFrequencyEveryDay(request.frequencyEveryDay());
        habit.setLocalDate(request.localDate());
        habit.setObjective(request.objective());
        habit.setObservation(request.observation());

        Habit habitSaved = habitRepository.save(habit);

        return new HabitResponseDTO(habitSaved);
    }

    public List<HabitResponseDTO> findAllHabits() {
        return habitRepository.findAllByUserId(currentUser.id()).stream()
                .map(HabitResponseDTO::new)
                .toList();
    }

    @Transactional
    public HabitResponseDTO toggleCompletion(Integer id, LocalDate date) {
        Habit habit = habitRepository.findLockedByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found: " + id));

        if (date.isAfter(LocalDate.now(clock)) || !metrics.scheduled(habit, date) || !habit.active())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit cannot be completed on this date");
        if (!habit.getCompletedDates().remove(date)) {
            habit.getCompletedDates().add(date);
        }

        return new HabitResponseDTO(habit);
    }

    public List<PlannerHabitDTO> plannerHabits(LocalDate date) {
        return habitRepository.findAllByUserId(currentUser.id()).stream()
                .filter(habit -> habit.active() && metrics.scheduled(habit, date))
                .map(habit -> new PlannerHabitDTO(habit.getId(), habit.getUserId(), habit.getName(), habit.getCompletedDates().contains(date)))
                .toList();
    }
}
