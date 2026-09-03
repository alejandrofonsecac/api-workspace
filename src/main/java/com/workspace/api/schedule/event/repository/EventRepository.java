package com.workspace.api.schedule.event.repository;

import com.workspace.api.schedule.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("scheduleEventRepository")
public interface EventRepository extends JpaRepository<Event, Integer> {
}
