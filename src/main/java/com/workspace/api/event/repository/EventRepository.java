package com.workspace.api.event.repository;

import java.util.Optional;
import com.workspace.api.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByUserId(String userId);
    Optional<Event> findByIdAndUserId(Long id, String userId);
    List<Event> findByUserIdOrderByDateAscStartTimeAsc(String userId);
    List<Event> findByUserIdAndDateOrderByStartTimeAsc(String userId, LocalDate date);

    boolean existsByUserIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            String userId, LocalDate date, LocalTime endTime, LocalTime startTime);

    boolean existsByUserIdAndDateAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            String userId, LocalDate date, LocalTime endTime, LocalTime startTime, Long id);
    long countByUserIdAndDate(String userId, LocalDate date);
}
