package com.workspace.api.tasks.service;

import com.workspace.api.tasks.domain.Tasks;
import com.workspace.api.tasks.dto.TasksRequestDTO;
import com.workspace.api.tasks.dto.TasksResponseDTO;
import com.workspace.api.tasks.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;

    public List<TasksResponseDTO> searchAllTasks(){
        return tasksRepository.findAll().stream()
                .map(TasksResponseDTO::new)
                .toList();
    }

    public TasksResponseDTO insertTask(TasksRequestDTO request){
        Tasks tasks = new Tasks();
        tasks.setTitle(request.title());
        tasks.setPriority(request.priority());
        tasks.setDueLabel(request.dueLabel());
        tasks.setCompleted(request.completed() != null && request.completed());
        tasks.setNotes(request.notes());

        tasks.setTaskCreatedDate(LocalDate.now());
        tasks.setDueDate(LocalDate.now());

        Tasks savedTask = tasksRepository.save(tasks);

        return new TasksResponseDTO(savedTask);
    }

    public TasksResponseDTO updateTask(Long id, TasksRequestDTO request){
        Tasks tasks = tasksRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        if (request.title() != null) tasks.setTitle(request.title());
        if (request.priority() != null) tasks.setPriority(request.priority());
        if (request.dueLabel() != null) tasks.setDueLabel(request.dueLabel());
        if (request.completed() != null) tasks.setCompleted(request.completed());
        if (request.notes() != null) tasks.setNotes(request.notes());

        Tasks updatedTask = tasksRepository.save(tasks);

        return new TasksResponseDTO(updatedTask);
    }

    public void deleteTask(Long id){
        if (!tasksRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id);
        }
        tasksRepository.deleteById(id);
    }

    public long countTasks(){
        return tasksRepository.countByDueDate(LocalDate.now());
    }
}
