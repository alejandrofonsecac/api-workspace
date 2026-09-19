package com.workspace.api.event.controller;

import com.workspace.api.event.service.CalendarQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar/events")
public class CalendarController {
    private final CalendarQueryService calendar;
    @GetMapping public List<CalendarQueryService.Entry> list() { return calendar.list(); }
}
