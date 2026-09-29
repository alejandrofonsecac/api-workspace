package com.workspace.api.pages.habit.repository;

import java.util.List;
import com.workspace.api.pages.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Integer> {
    List<Habit> findAllByUserId(String userId);
    Optional<Habit> findByIdAndUserId(Integer id, String userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habit h where h.id = :id and h.userId = :userId")
    Optional<Habit> findLockedByIdAndUserId(@Param("id") Integer id, @Param("userId") String userId);
}
