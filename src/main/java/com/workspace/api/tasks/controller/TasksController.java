package com.workspace.api.tasks.controller;

import com.workspace.api.tasks.dto.TasksDTO;
import com.workspace.api.tasks.service.TasksService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("tasks")
@CrossOrigin(origins = "http://localhost:4200")
public class TasksController {
    private TasksService tasksService;

    @GetMapping
    public List<TasksDTO> getTasks(){
        return tasksService.searchAllTasks();
    }

    @PostMapping
    public ResponseEntity<TasksDTO> saveTask(@RequestBody TasksDTO dto){
        TasksDTO savedTask = tasksService.insertTask(dto);
        return ResponseEntity.ok().body(savedTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TasksDTO> updateTask(@PathVariable Long id, @RequestBody TasksDTO dto){
        TasksDTO updatedTask = tasksService.updateTask(id, dto);
        return ResponseEntity.ok().body(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        tasksService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
