package com.workspace.api.schedule.event.controller;

import com.workspace.api.schedule.event.dto.EventResponseDTO;
import com.workspace.api.schedule.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("scheduleEventController")
@RequestMapping("/event")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
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
