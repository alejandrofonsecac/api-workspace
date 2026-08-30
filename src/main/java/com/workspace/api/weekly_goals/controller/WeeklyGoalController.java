package com.workspace.api.weekly_goals.controller;

import com.workspace.api.weekly_goals.dto.WeeklyGoalDTO;
import com.workspace.api.weekly_goals.service.WeeklyGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/weekly-goals")
@RequiredArgsConstructor
public class WeeklyGoalController {

    private final WeeklyGoalService weeklyGoalService;

    @GetMapping
    public List<WeeklyGoalDTO> getAll() {
        return weeklyGoalService.findAll();
    }

    @GetMapping("/{id}")
    public WeeklyGoalDTO getById(@PathVariable Integer id) {
        return weeklyGoalService.findById(id);
    }

    @PostMapping
    public ResponseEntity<WeeklyGoalDTO> create(@RequestBody WeeklyGoalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyGoalService.create(dto));
    }

    @PutMapping("/{id}")
    public WeeklyGoalDTO update(@PathVariable Integer id, @RequestBody WeeklyGoalDTO dto) {
        return weeklyGoalService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        weeklyGoalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
