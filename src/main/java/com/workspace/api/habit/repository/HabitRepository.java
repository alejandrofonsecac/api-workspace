package com.workspace.api.habit.repository;

import com.workspace.api.habit.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habit h where h.id = :id")
    Optional<Habit> findLockedById(@Param("id") Integer id);
}
