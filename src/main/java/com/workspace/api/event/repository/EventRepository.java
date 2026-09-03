package com.workspace.api.event.repository;

import com.workspace.api.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByDateAscStartTimeAsc();
    List<Event> findByDateOrderByStartTimeAsc(LocalDate date);

    boolean existsByDateAndStartTimeLessThanAndEndTimeGreaterThan(
            LocalDate date, LocalTime endTime, LocalTime startTime);

    boolean existsByDateAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            LocalDate date, LocalTime endTime, LocalTime startTime, Long id);
    long countByDate(LocalDate date);
}
