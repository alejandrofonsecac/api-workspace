package com.workspace.api.habit.controller;

import com.workspace.api.habit.dto.HabitRequestDTO;
import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("habit")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080")
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
}
