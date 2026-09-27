package com.workspace.api.schedule.tasks.repository;

import java.util.Optional;
import com.workspace.api.schedule.tasks.domain.Tasks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository("scheduleTasksRepository")
public interface TasksRepository extends JpaRepository<Tasks, Long> {
    List<Tasks> findAllByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Tasks item where item.id = :id and item.userId = :userId")
    Optional<Tasks> findForUpdateByIdAndUserId(Long id, String userId);

    Optional<Tasks> findByIdAndUserId(Long id, String userId);
    List<Tasks> findByUserIdAndTaskCreatedDate(String userId, LocalDate date);
    List<Tasks> findByUserIdAndDueDate(String userId, LocalDate date);

    long countByUserIdAndDueDate(String userId, LocalDate date);
    long countByUserIdAndTaskCreatedDate(String userId, LocalDate date);
}
