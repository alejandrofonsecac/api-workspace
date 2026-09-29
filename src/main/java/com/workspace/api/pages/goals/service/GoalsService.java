package com.workspace.api.pages.goals.service;

import com.workspace.api.pages.goals.dto.GoalFocusDTO;
import com.workspace.api.pages.goals.dto.GoalsBoardDTO;
import com.workspace.api.pages.goals.dto.GoalsRequestDTO;
import com.workspace.api.pages.goals.dto.GoalsResponseDTO;
import com.workspace.api.pages.goals.entity.GoalFocus;
import com.workspace.api.pages.goals.entity.GoalStatus;
import com.workspace.api.pages.goals.entity.Goals;
import com.workspace.api.pages.goals.repository.GoalFocusRepository;
import com.workspace.api.pages.goals.repository.GoalsRepository;
import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.goals.dto.*;
import com.workspace.api.goals.entity.*;
import com.workspace.api.goals.repository.*;
import com.workspace.api.util.domain.Category;
import com.workspace.api.util.domain.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalsService {
    private final GoalsRepository goalsRepository;
    private final CurrentUser currentUser;
    private final GoalFocusRepository focusRepository;
    private final Clock clock;

    public List<GoalsResponseDTO> getAllGoals() {
        return goalsRepository.findAllByUserId(currentUser.id()).stream().map(this::response).toList();
    }

    public GoalsResponseDTO getGoalById(Integer id) {
        return response(findGoal(id, false));
    }

    public GoalsBoardDTO board(String weekId, Category category, GoalStatus status) {
        String current = currentWeek();
        String selected = weekId == null ? current : weekId;
        LocalDate start = GoalWeeks.start(selected);

        List<GoalsResponseDTO> weekGoals = goalsRepository.findByUserIdAndWeekIdOrderByIdAsc(currentUser.id(), selected)
                .stream()
                .map(this::response)
                .toList();

        int completed = (int) weekGoals.stream()
                .filter(goal -> goal.status() == GoalStatus.COMPLETED).count();

        List<GoalsResponseDTO> filtered = weekGoals.stream()
                .filter(goal -> category == null || goal.category() == category)
                .filter(goal -> status == null || goal.status() == status)
                .toList();

        GoalFocusDTO focus = focusRepository.findByUserIdAndWeekId(currentUser.id(), selected)
                .map(value -> new GoalFocusDTO(value.getFocus(), value.getMessage())).orElse(null);

        var week = new GoalsBoardDTO.Week(selected, GoalWeeks.id(start.minusWeeks(1)),
                GoalWeeks.id(start.plusWeeks(1)), current, start, start.plusDays(6), selected.equals(current));

        var defaults = new GoalsRequestDTO("", "", Priority.NORMAL, Category.WORK, selected);

        var priorities = Arrays.stream(Priority.values()).map(value -> new GoalsBoardDTO.Option(value.name(),
                switch (value) { case LOW -> "Baixa"; case NORMAL -> "Média"; case HIGH -> "Alta"; })).toList();

        var categories = Arrays.stream(Category.values()).map(value -> new GoalsBoardDTO.Option(value.name(),
                switch (value) {
                    case WORK -> "Trabalho"; case STUDY -> "Estudo"; case HEALTH -> "Saúde";
                    case MIND -> "Mente"; case WELLBEING -> "Bem-estar";
                    case PERSONAL -> "Pessoal"; case PROJECT -> "Projetos";
                })).toList();

        return new GoalsBoardDTO(week, filtered,
                new GoalsBoardDTO.Summary(completed, weekGoals.size(), percentage(completed, weekGoals.size())),
                focus, defaults, new GoalsBoardDTO.Options(priorities, categories));
    }

    @Transactional
    public GoalsResponseDTO createGoals(GoalsRequestDTO request) {
        Goals goal = new Goals();
        goal.setUserId(currentUser.id());
        goal.setCreatedAt(clock.instant());
        goal.setWeekId(request.weekId() == null ? currentWeek() : request.weekId());
        applyRequest(goal, request);
        return response(goalsRepository.save(goal));
    }

    @Transactional
    public GoalsResponseDTO updateGoal(Integer id, GoalsRequestDTO request) {
        Goals goal = findGoal(id, true);
        if (request.weekId() != null && !request.weekId().equals(goal.getWeekId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the defer operation to move a goal to another week");
        }
        applyRequest(goal, request);
        return response(goal);
    }

    @Transactional
    public GoalsResponseDTO complete(Integer id) {
        Goals goal = findGoal(id, true);
        if (goal.getCompletedAt() == null) goal.setCompletedAt(clock.instant());
        return response(goal);
    }

    @Transactional
    public GoalsResponseDTO defer(Integer id) {
        Goals goal = findGoal(id, true);
        if (goal.getCompletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A completed goal cannot be deferred");
        }
        goal.setWeekId(GoalWeeks.id(GoalWeeks.start(goal.getWeekId()).plusWeeks(1)));
        goal.setDeferredCount(goal.getDeferredCount() + 1);
        return response(goal);
    }

    @Transactional
    public GoalFocusDTO saveFocus(String weekId, GoalFocusDTO request) {
        GoalWeeks.start(weekId);
        GoalFocus focus = focusRepository.findByUserIdAndWeekId(currentUser.id(), weekId).orElseGet(GoalFocus::new);
        focus.setWeekId(weekId);
        focus.setUserId(currentUser.id());
        focus.setFocus(request.focus().trim());
        focus.setMessage(request.message() == null ? "" : request.message().trim());
        focusRepository.save(focus);
        return new GoalFocusDTO(focus.getFocus(), focus.getMessage());
    }

    @Transactional
    public void deleteGoal(Integer id) {
        goalsRepository.delete(findGoal(id, true));
    }

    private Goals findGoal(Integer id, boolean lock) {
        return (lock ? goalsRepository.findLockedByIdAndUserId(id, currentUser.id()) : goalsRepository.findByIdAndUserId(id, currentUser.id()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found: " + id));
    }

    private void applyRequest(Goals goal, GoalsRequestDTO request) {
        if (goal.getWeekId() == null) goal.setWeekId(currentWeek());
        GoalWeeks.start(goal.getWeekId());
        goal.setGoalName(request.name().trim());
        goal.setExpectedTarget(request.expectedTarget().trim());
        goal.setPriority(request.priority());
        goal.setCategory(request.category());
    }

    private GoalsResponseDTO response(Goals goal) {
        boolean completed = goal.getCompletedAt() != null;
        String weekId = goal.getWeekId();
        LocalDate start = GoalWeeks.start(weekId == null ? currentWeek() : weekId);
        GoalStatus status = completed ? GoalStatus.COMPLETED
                : start.isAfter(LocalDate.now(clock)) ? GoalStatus.PLANNED : GoalStatus.IN_PROGRESS;
        return new GoalsResponseDTO(goal.getId(), goal.getUserId(), goal.getGoalName(), goal.getExpectedTarget(),
                goal.getPriority(), goal.getCategory(), weekId,
                status, goal.getCreatedAt(), goal.getCompletedAt(),
                goal.getDeferredCount(), !completed, !completed);
    }

    private String currentWeek() { return GoalWeeks.id(LocalDate.now(clock)); }

    private int percentage(int completed, int total) {
        return total == 0 ? 0 : (int) Math.min(100, Math.round(completed * 100.0 / total));
    }
}
