package com.workspace.api.tasks.controller;

import com.workspace.api.tasks.dto.TasksRequestDTO;
import com.workspace.api.tasks.dto.TasksResponseDTO;
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

    @GetMapping("/getTask")
    public List<TasksResponseDTO> getTasks(){
        return tasksService.searchAllTasks();
    }

    @GetMapping("/taskByDate")
    public long getTasksByDate(){
        return tasksService.countTasks();
    }

    @PostMapping
    public ResponseEntity<TasksResponseDTO> saveTask(@RequestBody TasksRequestDTO request){
        TasksResponseDTO savedTask = tasksService.insertTask(request);
        return ResponseEntity.ok().body(savedTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TasksResponseDTO> updateTask(@PathVariable Long id, @RequestBody TasksRequestDTO request){
        TasksResponseDTO updatedTask = tasksService.updateTask(id, request);
        return ResponseEntity.ok().body(updatedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id){
        tasksService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
