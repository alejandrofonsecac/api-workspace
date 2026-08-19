package com.workspace.api.event.controller;

import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public List<EventResponseDTO> getEvents() {
        return eventService.searchAllEvents();
    }

    @GetMapping("/getEventCount")
    public long getEventCount() {
        return eventService.countEvents();
    }

    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody EventResponseDTO dto) {
        EventResponseDTO event = eventService.insertEvent(dto);
        return  ResponseEntity.status(HttpStatus.CREATED).body(event);
    }
}
