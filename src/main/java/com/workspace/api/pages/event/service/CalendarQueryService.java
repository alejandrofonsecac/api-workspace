package com.workspace.api.pages.event.service;

import com.workspace.api.pages.event.dto.EventResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {
    private final EventService events;

    public List<EventResponseDTO> list() {
        return events.findAll()
                .stream()
                .filter(event ->
                        event.date() != null).toList();
    }
}
