package com.workspace.api.tasks.repository;

import com.workspace.api.tasks.domain.Tasks;
import com.workspace.api.tasks.dto.TasksDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TasksRepository extends JpaRepository<Tasks, Long> {
    List<Tasks> findByTaskCreatedDate(LocalDate date);
    List<Tasks> findByDueDate(LocalDate date);

    long countByDueDate(LocalDate date);
    long countByTaskCreatedDate(LocalDate date);
}
