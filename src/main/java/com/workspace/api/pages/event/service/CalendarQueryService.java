package com.workspace.api.pages.event.service;

import com.workspace.api.pages.event.dto.EventResponseDTO;
import com.workspace.api.pages.event.repository.EventRepository;
import com.workspace.api.user.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {
    private final EventRepository events;
    private final CurrentUser currentUser;

    public List<EventResponseDTO> list() {
        return events.findByUserIdAndDateIsNotNullOrderByDateAscStartTimeAsc(currentUser.id()).stream()
                .map(EventResponseDTO::new)
                .toList();
    }
}
