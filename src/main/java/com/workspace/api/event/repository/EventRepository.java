package com.workspace.api.event.repository;

import java.util.Optional;

import com.workspace.api.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByScheduledTaskIdAndUserId(Long taskId, String userId);

    List<Event> findAllByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Event item where item.id = :id and item.userId = :userId")
    Optional<Event> findForUpdateByIdAndUserId(Long id, String userId);

    Optional<Event> findByIdAndUserId(Long id, String userId);

    List<Event> findByUserIdOrderByDateAscStartTimeAsc(String userId);

    List<Event> findByUserIdAndDateOrderByStartTimeAsc(String userId, LocalDate date);

    long countByUserIdAndDate(String userId, LocalDate date);
}
