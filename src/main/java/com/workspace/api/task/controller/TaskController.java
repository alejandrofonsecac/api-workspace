package com.workspace.api.task.controller;

import com.workspace.api.task.entity.TaskResponseDTO;
import com.workspace.api.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    private List<TaskResponseDTO> findAllTask(){
        return taskService.findAllTask();
    }

    @PostMapping
    private ResponseEntity<TaskResponseDTO> createTask(@RequestBody TaskResponseDTO dto){
        TaskResponseDTO taskSaved = taskService.createTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskSaved);
    }
}
