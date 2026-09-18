package com.workspace.api.schedule.tasks.service;

import com.workspace.api.user.security.CurrentUser;
import com.workspace.api.schedule.tasks.domain.Tasks;
import com.workspace.api.schedule.tasks.dto.TasksDTO;
import com.workspace.api.schedule.tasks.repository.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service("scheduleTasksService")
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;
    private final CurrentUser currentUser;
    private final java.time.Clock clock;
    private final com.workspace.api.user.repository.UserRepository users;

    public List<TasksDTO> searchAllTasks() {
        return tasksRepository.findAllByUserId(currentUser.id()).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public TasksDTO insertTask(TasksDTO dto) {
        lockUser();
        Tasks tasks = new Tasks();
        tasks.setUserId(currentUser.id());
        validateTitle(dto.title());
        tasks.setTitle(dto.title().trim());
        tasks.setPriority(dto.priority());
        tasks.setDueLabel(null);
        tasks.setCompleted(dto.completed() != null && dto.completed());
        tasks.setNotes(dto.notes());

        tasks.setTaskCreatedDate(LocalDate.now(clock));
        tasks.setDueDate(dto.dueDate());
        tasks.setDueTime(dto.dueTime());

        Tasks savedTask = tasksRepository.save(tasks);

        return response(savedTask);
    }

    @Transactional
    public TasksDTO updateTask(Long id, TasksDTO dto) {
        lockUser();
        Tasks tasks = tasksRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));

        if (dto.title() != null) {
            validateTitle(dto.title());
            tasks.setTitle(dto.title().trim());
        }
        if (dto.priority() != null) tasks.setPriority(dto.priority());
        if (dto.dueDate() != null) tasks.setDueDate(dto.dueDate());
        if (dto.dueTime() != null) tasks.setDueTime(dto.dueTime());
        if (dto.completed() != null) tasks.setCompleted(dto.completed());
        if (dto.notes() != null) tasks.setNotes(dto.notes());

        Tasks updatedTask = tasksRepository.save(tasks);

        return response(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        lockUser();
        Tasks task = tasksRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + id));
        tasksRepository.delete(task);
    }

    private void lockUser() {
        users.lockById(currentUser.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task title of up to 255 characters is required");
    }

    private TasksDTO response(Tasks task) {
        String label = "Sem data";
        if (task.getDueDate() != null) {
            var today = LocalDate.now(clock);
            label = task.getDueDate().equals(today) ? "Hoje" : task.getDueDate().equals(today.plusDays(1)) ? "Amanhã" : task.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        if (task.getDueTime() != null) label += ", " + task.getDueTime();
        return new TasksDTO(task.getId(), task.getUserId(), task.getTitle(), task.getPriority(), label, task.getCompleted(), task.getNotes(), task.getDueDate(), task.getDueTime());
    }

    public long countTasks() {
        return tasksRepository.countByUserIdAndDueDate(currentUser.id(), LocalDate.now(clock));
    }
}
