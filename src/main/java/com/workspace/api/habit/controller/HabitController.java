package com.workspace.api.habit.controller;

import com.workspace.api.habit.dto.HabitRequestDTO;
import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.dto.PlannerHabitDTO;
import com.workspace.api.habit.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("habit")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class HabitController {
    public final HabitService habitService;

    @PostMapping
    public ResponseEntity<HabitResponseDTO> createHabit(@RequestBody HabitRequestDTO request){
        HabitResponseDTO createdHabit = habitService.createHabit(request);
        return  ResponseEntity.status(HttpStatus.CREATED).body(createdHabit);
    }

    @GetMapping
    public List<HabitResponseDTO> getAllHabit(){
        return habitService.findAllHabits();
    }

    @PostMapping("/{id}/completions/{date}/toggle")
    public HabitResponseDTO toggleCompletion(@PathVariable Integer id, @PathVariable LocalDate date) {
        return habitService.toggleCompletion(id, date);
    }

    @GetMapping("/planner")
    public List<PlannerHabitDTO> plannerHabits(@RequestParam LocalDate date) {
        return habitService.plannerHabits(date);
    }
}
