package com.workspace.api.schedule.tasks.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.schedule.tasks.domain.Tasks;
import com.workspace.api.schedule.tasks.dto.TasksDTO;
import com.workspace.api.schedule.tasks.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service("scheduleTasksService")
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;
    private final CurrentUser currentUser;

    public List<TasksDTO> searchAllTasks(){
        return tasksRepository.findAllByUserId(currentUser.id()).stream()
                .map(TasksDTO::new)
                .toList();
    }

    public TasksDTO insertTask(TasksDTO dto){
        Tasks tasks = new Tasks();
        tasks.setUserId(currentUser.id());
        tasks.setTitle(dto.title());
        tasks.setPriority(dto.priority());
        tasks.setDueLabel(dto.dueLabel());
        tasks.setCompleted(dto.completed() != null && dto.completed());
        tasks.setNotes(dto.notes());

        tasks.setTaskCreatedDate(LocalDate.now());
        tasks.setDueDate(LocalDate.now());

        Tasks savedTask = tasksRepository.save(tasks);

        return new TasksDTO(savedTask);
    }

    public TasksDTO updateTask(Long id, TasksDTO dto){
        Tasks tasks = tasksRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        if (dto.title() != null) tasks.setTitle(dto.title());
        if (dto.priority() != null) tasks.setPriority(dto.priority());
        if (dto.dueLabel() != null) tasks.setDueLabel(dto.dueLabel());
        if (dto.completed() != null) tasks.setCompleted(dto.completed());
        if (dto.notes() != null) tasks.setNotes(dto.notes());

        Tasks updatedTask = tasksRepository.save(tasks);

        return new TasksDTO(updatedTask);
    }

    public void deleteTask(Long id){
        Tasks task = tasksRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        tasksRepository.delete(task);
    }

    public long countTasks(){
        return tasksRepository.countByUserIdAndDueDate(currentUser.id(), LocalDate.now());
    }
}
