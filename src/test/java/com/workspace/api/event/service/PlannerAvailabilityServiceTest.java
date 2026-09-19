package com.workspace.api.event.service;

import com.workspace.api.event.domain.EventCategory;
import com.workspace.api.event.domain.EventSource;
import com.workspace.api.event.dto.EventRequestDTO;
import com.workspace.api.planner.dto.PlannerAvailabilityResponseDTO;
import com.workspace.api.planner.exception.InvalidPlannerSlotException;
import com.workspace.api.planner.exception.OccupiedPlannerSlotException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({EventService.class, com.workspace.api.user.security.CurrentUser.class, com.workspace.api.goals.service.GoalsConfiguration.class})
@ActiveProfiles("test")
class PlannerAvailabilityServiceTest extends com.workspace.api.support.AuthenticatedTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 31);

    @Autowired
    private EventService eventService;

    @Test
    void dayWithoutEventsReturnsTwentyFourAvailableHours() {
        PlannerAvailabilityResponseDTO availability = eventService.getAvailability(DATE);

        assertEquals(DATE, availability.date());
        assertEquals(24, availability.totalBlocks());
        assertEquals(0, availability.occupiedBlocks());
        assertEquals(24, availability.availableBlocks());
        assertEquals(24, availability.availableHours());
        assertEquals(24, availability.slots().size());
        assertTrue(availability.slots().stream().allMatch(slot -> slot.available()));
    }

    @Test
    void eachValidEventOccupiesExactlyOneHour() {
        eventService.create(request(LocalTime.of(8, 0)));
        eventService.create(request(LocalTime.of(12, 0)));
        eventService.create(request(LocalTime.of(19, 0)));

        PlannerAvailabilityResponseDTO availability = eventService.getAvailability(DATE);

        assertEquals(3, availability.occupiedBlocks());
        assertEquals(21, availability.availableBlocks());
        assertEquals(21, availability.availableHours());
        assertFalse(availability.slots().get(8).available());
        assertFalse(availability.slots().get(12).available());
        assertFalse(availability.slots().get(19).available());
    }

    @Test
    void creatingEventInOccupiedBlockReturnsConflict() {
        eventService.create(request(LocalTime.of(9, 0)));

        OccupiedPlannerSlotException exception = assertThrows(
                OccupiedPlannerSlotException.class,
                () -> eventService.create(request(LocalTime.of(9, 0)))
        );

        assertEquals("This time slot is already occupied for the selected date.", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidTimeRanges")
    void invalidSlotsAreRejected(LocalTime startTime, LocalTime endTime) {
        assertThrows(InvalidPlannerSlotException.class,
                () -> eventService.create(request(startTime, endTime)));
    }

    private static Stream<Arguments> invalidTimeRanges() {
        return Stream.of(
                Arguments.of(LocalTime.of(9, 0), LocalTime.of(8, 0)),
                Arguments.of(LocalTime.of(20, 0), LocalTime.of(20, 0)),
                Arguments.of(LocalTime.of(9, 30, 1), LocalTime.of(10, 30)),
                Arguments.of(LocalTime.of(9, 0), LocalTime.of(10, 0, 1))
        );
    }

    private EventRequestDTO request(LocalTime startTime) {
        return request(startTime, startTime.plusHours(1));
    }

    private EventRequestDTO request(LocalTime startTime, LocalTime endTime) {
        return new EventRequestDTO(
                "Planner event",
                DATE,
                startTime,
                endTime,
                null,
                null,
                EventSource.internal,
                EventCategory.work
        );
    }
}
