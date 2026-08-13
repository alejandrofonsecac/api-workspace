package com.workspace.api.event.controller;

import com.workspace.api.event.domain.Event;
import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public List<EventResponseDTO> getEvents() {
        return eventService.searchAllEvents();
    }

    public ResponseEntity<EventResponseDTO> getEventsByName(@RequestBody EventResponseDTO dto) {

    }
}
