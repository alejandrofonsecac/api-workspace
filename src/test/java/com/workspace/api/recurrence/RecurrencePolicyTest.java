package com.workspace.api.recurrence;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class RecurrencePolicyTest {
    @Test void monthlyKeepsAnchorAndSkipsImpossibleDates() {
        assertThat(RecurrencePolicy.dates(LocalDate.parse("2030-01-31"), true, "FREQ=MONTHLY;UNTIL=20300531"))
                .containsExactly(LocalDate.parse("2030-01-31"), LocalDate.parse("2030-03-31"), LocalDate.parse("2030-05-31"));
    }
    @Test void leapDayReturnsInTheNextLeapYear() {
        assertThat(RecurrencePolicy.dates(LocalDate.parse("2028-02-29"), true, "FREQ=YEARLY;UNTIL=20320301"))
                .containsExactly(LocalDate.parse("2028-02-29"), LocalDate.parse("2032-02-29"));
    }
    @Test void weeklyCrossesYearAndIncludesEndDate() {
        assertThat(RecurrencePolicy.dates(LocalDate.parse("2026-12-28"), true, "FREQ=WEEKLY;UNTIL=20270111"))
                .containsExactly(LocalDate.parse("2026-12-28"), LocalDate.parse("2027-01-04"), LocalDate.parse("2027-01-11"));
    }
    @Test void weekdaysOnlySkipsWeekendsIncludingAWeekendStart() {
        assertThat(RecurrencePolicy.dates(LocalDate.parse("2030-01-05"), true, "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20300114"))
                .containsExactly(LocalDate.parse("2030-01-07"), LocalDate.parse("2030-01-08"), LocalDate.parse("2030-01-09"),
                        LocalDate.parse("2030-01-10"), LocalDate.parse("2030-01-11"), LocalDate.parse("2030-01-14"));
    }
    @Test void weekdaysOnlyCountsOnlyCreatedOccurrencesTowardTheLimit() {
        // 2030-01-01 to 2033-10-31 spans 1400 days and exactly 1000 weekdays.
        assertThat(RecurrencePolicy.dates(LocalDate.parse("2030-01-01"), true, "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20331031"))
                .hasSize(1000).noneMatch(date -> date.getDayOfWeek().getValue() > 5);
        assertThatThrownBy(() -> RecurrencePolicy.dates(LocalDate.parse("2030-01-01"), true, "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20331101"))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsWeekdaysOnlyOutsideABoundedDailySeries() {
        for (String rule : new String[]{"FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20300131", "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR",
                "FREQ=DAILY;BYDAY=MO,WE;UNTIL=20300131", "FREQ=DAILY;UNTIL=20300131;BYDAY=MO,TU,WE,TH,FR"}) {
            assertThatThrownBy(() -> RecurrencePolicy.dates(LocalDate.parse("2030-01-01"), true, rule)).isInstanceOf(ResponseStatusException.class);
        }
        // A weekend-only period would create nothing.
        assertThatThrownBy(() -> RecurrencePolicy.dates(LocalDate.parse("2030-01-05"), true, "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR;UNTIL=20300106"))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsInvalidDatesRulesAndUnboundedWork() {
        for (String rule : new String[]{"FREQ=HOURLY;UNTIL=20300101", "FREQ=DAILY;UNTIL=20300230", "FREQ=DAILY;UNTIL=20290101", "FREQ=DAILY;UNTIL=20410101", "FREQ=DAILY;UNTIL=20350101"}) {
            assertThatThrownBy(() -> RecurrencePolicy.dates(LocalDate.parse("2030-01-01"), true, rule)).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> RecurrencePolicy.dates(null, true, "FREQ=WEEKLY;UNTIL=20301231")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void legacyMetadataAndUndatedEventsRemainCompatible() {
        assertThat(RecurrencePolicy.dates(null, true, "FREQ=WEEKLY")).hasSize(1).containsNull();
        assertThat(RecurrencePolicy.dates(null, false, null)).hasSize(1).containsNull();
    }
}
