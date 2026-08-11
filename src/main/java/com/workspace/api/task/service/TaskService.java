package com.workspace.api.task.service;

import com.workspace.api.task.entity.Task;
import com.workspace.api.task.entity.TaskResponseDTO;
import com.workspace.api.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    public List<TaskResponseDTO> findAllTask() {
        return taskRepository.findAll().stream()
                .map(TaskResponseDTO::new)
                .toList();
    }

    public TaskResponseDTO createTask(TaskResponseDTO dto){
        Task task = new Task();
        task.setId(dto.id());
        task.setName(dto.name());
        task.setDate(dto.date());
        task.setExecutionTime(dto.executionTime());
        task.setTimeEstimated(dto.timeEstimated());
        task.setTaskPriority(dto.taskPriority());
        task.setNote(dto.note());
        task.setCompleted(dto.completed());

        Task taskSaved = taskRepository.save(task);

        return new TaskResponseDTO(taskSaved);
    }
}
