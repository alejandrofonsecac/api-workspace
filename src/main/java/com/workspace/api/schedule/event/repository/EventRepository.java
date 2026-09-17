package com.workspace.api.schedule.event.repository;

import java.util.Optional;
import java.util.List;
import com.workspace.api.schedule.event.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("scheduleEventRepository")
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findAllByUserId(String userId);
    Optional<Event> findByIdAndUserId(Integer id, String userId);
    long countByUserId(String userId);
}
