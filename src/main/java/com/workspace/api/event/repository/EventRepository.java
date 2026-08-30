package com.workspace.api.event.repository;

import com.workspace.api.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByDateAscStartTimeAsc();
    long countByDate(LocalDate date);
}
