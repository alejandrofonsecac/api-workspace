package com.workspace.api.event.repository;

import com.workspace.api.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    long countByDueDate(LocalDate date);
}
