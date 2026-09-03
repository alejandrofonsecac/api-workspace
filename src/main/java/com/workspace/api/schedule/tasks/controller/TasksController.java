package com.workspace.api.schedule.tasks.controller;

import com.workspace.api.schedule.tasks.dto.TasksDTO;
import com.workspace.api.schedule.tasks.service.TasksService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController("scheduleTasksController")
@RequestMapping("/schedule/tasks")
@CrossOrigin(origins = "http://localhost:4200")
public class TasksController {
    private TasksService tasksService;

    @GetMapping("/getTask")
    public List<TasksDTO> getTasks(){
        return tasksService.searchAllTasks();
    }

    @GetMapping("/taskByDate")
    public long getTasksByDate(){
        return tasksService.countTasks();
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
