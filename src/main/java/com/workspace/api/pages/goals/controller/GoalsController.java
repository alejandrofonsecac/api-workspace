package com.workspace.api.pages.goals.controller;

import com.workspace.api.pages.goals.dto.GoalsRequestDTO;
import com.workspace.api.pages.goals.dto.GoalsResponseDTO;
import com.workspace.api.pages.goals.dto.GoalsBoardDTO;
import com.workspace.api.pages.goals.dto.GoalFocusDTO;
import com.workspace.api.pages.goals.entity.GoalStatus;
import com.workspace.api.util.domain.Category;
import com.workspace.api.pages.goals.service.GoalsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/goals")
public class GoalsController {
    private final GoalsService goalsService;

    @GetMapping("/board")
    public GoalsBoardDTO board(@RequestParam(required = false) String weekId,
                               @RequestParam(required = false) Category category,
                               @RequestParam(required = false) GoalStatus status) {
        return goalsService.board(weekId, category, status);
    }

    @PostMapping("/{id}/complete")
    public GoalsResponseDTO complete(@PathVariable Integer id) { return goalsService.complete(id); }

    @PostMapping("/{id}/defer")
    public GoalsResponseDTO defer(@PathVariable Integer id) { return goalsService.defer(id); }

    @PutMapping("/focus/{weekId}")
    public GoalFocusDTO saveFocus(@PathVariable String weekId, @Valid @RequestBody GoalFocusDTO request) {
        return goalsService.saveFocus(weekId, request);
    }

    @GetMapping
    public List<GoalsResponseDTO> getAllGoals(){
        return goalsService.getAllGoals();
    }

    @GetMapping("/{id}")
    public GoalsResponseDTO getGoalById(@PathVariable Integer id) {
        return goalsService.getGoalById(id);
    }

    @PostMapping
    public ResponseEntity<GoalsResponseDTO> createGoal(@Valid @RequestBody GoalsRequestDTO request){
        GoalsResponseDTO goal = goalsService.createGoals(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(goal);
    }

    @PutMapping("/{id}")
    public GoalsResponseDTO updateGoal(@PathVariable Integer id, @Valid @RequestBody GoalsRequestDTO request) {
        return goalsService.updateGoal(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Integer id) {
        goalsService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
