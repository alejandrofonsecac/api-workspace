package com.workspace.api.tasks.service;

import com.workspace.api.tasks.domain.Tasks;
import com.workspace.api.tasks.dto.TasksDTO;
import com.workspace.api.tasks.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;

    public List<TasksDTO> searchAllTasks(){
        return tasksRepository.findAll().stream()
                .map(TasksDTO::new)
                .toList();
    }

    public TasksDTO insertTask(TasksDTO dto){
        Tasks tasks = new Tasks();
        tasks.setTitle(dto.title());
        tasks.setPriority(dto.priority());
        tasks.setDueLabel(dto.dueLabel());
        tasks.setPeriod(dto.period());
        tasks.setProject(dto.project());
        tasks.setCompleted(dto.completed() != null && dto.completed());
        tasks.setNotes(dto.notes());
        tasks.setAssignee(dto.assignee());

        Tasks savedTask = tasksRepository.save(tasks);

        return new TasksDTO(savedTask);
    }

    public TasksDTO updateTask(Long id, TasksDTO dto){
        Tasks tasks = tasksRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        if (dto.title() != null) tasks.setTitle(dto.title());
        if (dto.priority() != null) tasks.setPriority(dto.priority());
        if (dto.dueLabel() != null) tasks.setDueLabel(dto.dueLabel());
        if (dto.period() != null) tasks.setPeriod(dto.period());
        if (dto.project() != null) tasks.setProject(dto.project());
        if (dto.completed() != null) tasks.setCompleted(dto.completed());
        if (dto.notes() != null) tasks.setNotes(dto.notes());
        if (dto.assignee() != null) tasks.setAssignee(dto.assignee());

        Tasks updatedTask = tasksRepository.save(tasks);

        return new TasksDTO(updatedTask);
    }

    public void deleteTask(Long id){
        if (!tasksRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id);
        }
        tasksRepository.deleteById(id);
    }
}
