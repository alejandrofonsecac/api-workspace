package com.workspace.api.goals.controller;

import com.workspace.api.goals.dto.GoalsRequestDTO;
import com.workspace.api.goals.dto.GoalsResponseDTO;
import com.workspace.api.goals.service.GoalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/goals")
@CrossOrigin(origins = "http://localhost:4200")
public class GoalsController {
    private final GoalsService goalsService;

    @GetMapping
    public List<GoalsResponseDTO> getAllGoals(){
        return goalsService.getAllGoals();
    }

    @PostMapping
    public ResponseEntity<GoalsResponseDTO> createGoal(@RequestBody GoalsRequestDTO request){
        GoalsResponseDTO goal = goalsService.createGoals(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(goal);
    }
}
