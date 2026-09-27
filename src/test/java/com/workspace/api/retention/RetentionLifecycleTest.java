package com.workspace.api.retention;

import com.workspace.api.event.domain.Event;
import com.workspace.api.note.domain.Note;
import com.workspace.api.retention.domain.RetainedEntity;
import com.workspace.api.schedule.tasks.domain.Tasks;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionLifecycleTest {
    static Stream<RetainedEntity> resources() { return Stream.of(new Tasks(), new Event(), new Note()); }

    @ParameterizedTest
    @MethodSource("resources")
    void completionIsStableUntilReopened(RetainedEntity item) {
        Instant first = Instant.parse("2026-09-01T12:00:00Z");
        Instant later = first.plusSeconds(3600);
        item.updateRetention(true, null, null, first);
        assertThat(item.getCompletedAt()).isEqualTo(first);
        item.updateRetention(true, null, null, later);
        item.updateRetention(null, null, null, later);
        assertThat(item.getCompletedAt()).isEqualTo(first);
        item.updateRetention(false, null, null, later);
        assertThat(item.getCompletedAt()).isNull();
        item.updateRetention(true, null, null, later);
        assertThat(item.getCompletedAt()).isEqualTo(later);
    }

    @ParameterizedTest
    @MethodSource("resources")
    void unknownLegacyCompletionIsNotInventedAndRecurrenceSurvivesOmission(RetainedEntity item) {
        item.setCompleted(true);
        item.updateRetention(true, true, " FREQ=DAILY ", Instant.now());
        assertThat(item.getCompletedAt()).isNull();
        item.updateRetention(null, null, null, Instant.now());
        assertThat(item.isRecurring()).isTrue();
        assertThat(item.getRecurrenceRule()).isEqualTo("FREQ=DAILY");
        item.updateRetention(null, false, " ", Instant.now());
        assertThat(item.isRecurring()).isFalse();
        assertThat(item.getRecurrenceRule()).isNull();
    }
}
