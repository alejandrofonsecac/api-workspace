package com.workspace.api.retention;

import com.workspace.api.retention.repository.RetentionRepository.Resource;
import com.workspace.api.retention.service.RetentionBatchService;
import com.workspace.api.retention.service.RetentionJob;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

class RetentionJobTest {
    private final RetentionBatchService batches = mock(RetentionBatchService.class);
    private final Instant now = Instant.parse("2026-09-27T06:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneId.of("America/Sao_Paulo"));
    private final Instant cutoff = now.minus(7, ChronoUnit.DAYS);

    @Test
    void drainsFullBatchesAndStopsOnPartialBatchForEachResource() {
        for (Resource resource : Resource.values()) when(batches.deleteBatch(resource, cutoff, 2)).thenReturn(2, 2, 1);
        new RetentionJob(batches, clock, 2, 100).cleanCompletedItems();
        for (Resource resource : Resource.values()) verify(batches, times(3)).deleteBatch(resource, cutoff, 2);
        verifyNoMoreInteractions(batches);
    }

    @Test
    void limitsDailyWorkAndContinuesOtherResourcesAfterFailure() {
        when(batches.deleteBatch(Resource.EVENTS, cutoff, 2)).thenReturn(2).thenThrow(new IllegalStateException("test failure"));
        when(batches.deleteBatch(Resource.NOTES, cutoff, 2)).thenReturn(2);
        new RetentionJob(batches, clock, 2, 3).cleanCompletedItems();
        verify(batches, times(2)).deleteBatch(Resource.EVENTS, cutoff, 2);
        verify(batches, times(3)).deleteBatch(Resource.NOTES, cutoff, 2);
    }

    @Test
    void rejectsUnboundedOrInvalidBatchConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RetentionJob(batches, clock, 0, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RetentionJob(batches, clock, 10001, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RetentionJob(batches, clock, 500, 0));
    }
}
