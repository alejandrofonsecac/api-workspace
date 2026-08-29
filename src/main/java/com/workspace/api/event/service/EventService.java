package com.workspace.api.event.service;

import com.workspace.api.event.domain.Event;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {
        return eventRepository.findAllByOrderByDateAscStartTimeAsc().stream()
                .map(EventResponseDTO::new)
                .toList();
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO request) {
        Event event = new Event();
        applyRequest(event, request);
        return new EventResponseDTO(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO request) {
        Event event = findEntityById(id);
        applyRequest(event, request);
        return new EventResponseDTO(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        Event event = findEntityById(id);
        eventRepository.delete(event);
    }

    private Event findEntityById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + id));
    }

    private void applyRequest(Event event, EventRequestDTO request) {
        event.setTitle(request.title());
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setSource(request.source());
        event.setCategory(request.category());
    }

    public long countEvents() {
        return eventRepository.countByDueDate(LocalDate.now());
    }
}
