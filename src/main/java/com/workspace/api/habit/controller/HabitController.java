package com.workspace.api.habit.controller;

import com.workspace.api.habit.dto.HabitResponseDTO;
import com.workspace.api.habit.service.HabitService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("habit")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:8080")
public class HabitController {
    public HabitService habitService;

    @PostMapping
    public ResponseEntity<HabitResponseDTO> createHabit(@RequestBody HabitResponseDTO dto){
        HabitResponseDTO createdHabit = habitService.createHabit(dto);
        return  ResponseEntity.ok().body(habitService.createHabit(createdHabit));
    }
}
