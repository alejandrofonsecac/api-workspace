package com.workspace.api.event.service;

import com.workspace.api.event.domain.Event;
import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public List<EventResponseDTO> searchAllEvents() {
        return eventRepository.findAll().stream()
                .map(EventResponseDTO::new)
                .toList();
    }

    public EventResponseDTO insertEvent(EventResponseDTO dto) {
        Event event = new Event();
        event.setId(dto.id());
        event.setName(dto.name());
        event.setDescription(dto.description());
        event.setStartTime(dto.startTime());
        event.setEndTime(dto.endTime());
        event.setCategory(dto.category());

        Event eventSaved = eventRepository.save(event);

        return new EventResponseDTO(eventSaved);
    }
}
