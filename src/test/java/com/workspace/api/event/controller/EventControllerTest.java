package com.workspace.api.event.controller;

import com.workspace.api.config.CorsConfig;
import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.event.dto.EventResponseDTO;
import com.workspace.api.event.service.EventService;
import com.workspace.api.planner.controller.PlannerExceptionHandler;
import com.workspace.api.planner.exception.OccupiedPlannerSlotException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({CorsConfig.class, PlannerExceptionHandler.class, com.workspace.api.user.security.SecurityConfiguration.class})
@org.springframework.security.test.context.support.WithMockUser
class EventControllerTest {
    @MockitoBean com.workspace.api.user.repository.UserRepository users;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    private static final String VALID_EVENT_JSON = """
            {
              "title": "Alignment meeting",
              "date": "2026-08-24",
              "startTime": "09:00",
              "endTime": "10:00",
              "description": "Define next steps",
              "location": null,
              "source": "internal",
              "category": "work"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void getEventsReturnsEventsOrderedByService() throws Exception {
        EventResponseDTO first = event(1L, "First", LocalDate.of(2026, 8, 24), LocalTime.of(9, 0));
        EventResponseDTO second = event(2L, "Second", LocalDate.of(2026, 8, 24), LocalTime.of(11, 0));
        given(eventService.findAll()).willReturn(List.of(first, second));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[0].date").value("2026-08-24"))
                .andExpect(jsonPath("$[0].startTime").value("09:00"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void postEventCreatesAndReturnsTheCompleteEvent() throws Exception {
        EventResponseDTO created = event(10L, "Alignment meeting", LocalDate.of(2026, 8, 24), LocalTime.of(9, 0));
        given(eventService.create(any(EventRequestDTO.class))).willReturn(created);

        mockMvc.perform(post("/planner/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Alignment meeting"))
                .andExpect(jsonPath("$.location").value(nullValue()))
                .andExpect(jsonPath("$.source").value("internal"))
                .andExpect(jsonPath("$.category").value("work"));
    }

    @Test
    void putEventUpdatesAndReturnsTheEvent() throws Exception {
        EventResponseDTO updated = event(10L, "Updated meeting", LocalDate.of(2026, 8, 24), LocalTime.of(9, 0));
        given(eventService.update(eq(10L), any(EventRequestDTO.class))).willReturn(updated);

        mockMvc.perform(put("/planner/events/{id}", 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON.replace("Alignment meeting", "Updated meeting")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Updated meeting"));
    }

    @Test
    void deleteEventReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/planner/events/{id}", 10))
                .andExpect(status().isNoContent());

        verify(eventService).delete(10L);
    }

    @Test
    void returnsNotFoundWhenServiceCannotFindEvent() throws Exception {
        given(eventService.update(eq(999L), any(EventRequestDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: 999"));

        mockMvc.perform(put("/planner/events/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnEndTimeThatIsNotAfterStartTime() throws Exception {
        String invalidTimes = VALID_EVENT_JSON
                .replace("\"startTime\": \"09:00\"", "\"startTime\": \"10:00\"")
                .replace("\"endTime\": \"10:00\"", "\"endTime\": \"09:00\"");

        mockMvc.perform(post("/planner/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTimes))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("increasing interval")));

        verifyNoInteractions(eventService);
    }

    @ParameterizedTest
    @MethodSource("invalidPlannerSlots")
    void rejectsPlannerSlotsOutsideTheDailySchedule(String eventJson) throws Exception {
        mockMvc.perform(post("/planner/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isUnprocessableContent());

        verifyNoInteractions(eventService);
    }

    @Test
    void rejectsADateOutsideTheExpectedFormat() throws Exception {
        mockMvc.perform(post("/planner/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON.replace("2026-08-24", "24/08/2026")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("date must use")));

        verifyNoInteractions(eventService);
    }

    @Test
    void rejectsAnOccupiedPlannerSlotWithConflict() throws Exception {
        given(eventService.create(any(EventRequestDTO.class)))
                .willThrow(new OccupiedPlannerSlotException("This time slot is already occupied for the selected date."));

        mockMvc.perform(post("/planner/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This time slot is already occupied for the selected date."));
    }

    @Test
    void allowsAngularCorsPreflight() throws Exception {
        mockMvc.perform(options("/events")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    private EventResponseDTO event(Long id, String title, LocalDate date, LocalTime startTime) {
        return new EventResponseDTO(
                id,
                "test-user",
                title,
                date,
                startTime,
                startTime.plusHours(1),
                "Define next steps",
                null,
                EventSource.internal,
                EventCategory.work
        );
    }

    private static Stream<Arguments> invalidPlannerSlots() {
        return Stream.of(
                Arguments.of(VALID_EVENT_JSON
                        .replace("\"startTime\": \"09:00\"", "\"startTime\": \"10:00\"")
                        .replace("\"endTime\": \"10:00\"", "\"endTime\": \"09:00\"")),
                Arguments.of(VALID_EVENT_JSON
                        .replace("\"startTime\": \"09:00\"", "\"startTime\": \"09:30:01\"")
                        .replace("\"endTime\": \"10:00\"", "\"endTime\": \"10:30\"")),
                Arguments.of(VALID_EVENT_JSON
                        .replace("\"endTime\": \"10:00\"", "\"endTime\": \"09:00\""))
        );
    }
}
