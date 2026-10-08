package com.workspace.api.user;

import com.workspace.api.user.domain.RefreshToken;
import com.workspace.api.user.repository.RefreshTokenRepository;
import com.workspace.api.user.service.RefreshSessionService;
import com.workspace.api.user.service.RefreshTokenCleanupJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles("test")
class RefreshTokenCleanupTest {
    private static final Instant NOW = Instant.parse("2026-10-08T06:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired RefreshTokenRepository tokens;

    @Test
    void deletesOnlyFamiliesPastTheirAbsoluteDeadline() {
        String expiredFamily = UUID.randomUUID().toString();
        String activeFamily = UUID.randomUUID().toString();
        var expiredUsed = save(expiredFamily, NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(3)), true);
        var expiredCurrent = save(expiredFamily, NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(2)), false);
        var withinGrace = save(UUID.randomUUID().toString(), NOW.minus(Duration.ofHours(1)), NOW.minus(Duration.ofHours(1)), false);
        // Used hashes of a family that may still refresh are needed to detect replay.
        var activeUsed = save(activeFamily, NOW.plus(Duration.ofDays(60)), NOW.minus(Duration.ofDays(40)), true);
        var idleExpired = save(activeFamily, NOW.plus(Duration.ofDays(60)), NOW.minus(Duration.ofDays(1)), false);

        new RefreshTokenCleanupJob(tokens, CLOCK, 1, 10).deleteExpiredFamilies();

        assertThat(tokens.findById(expiredUsed.getId())).isEmpty();
        assertThat(tokens.findById(expiredCurrent.getId())).isEmpty();
        assertThat(tokens.findById(withinGrace.getId())).isPresent();
        assertThat(tokens.findById(activeUsed.getId())).isPresent();
        assertThat(tokens.findById(idleExpired.getId())).isPresent();
    }

    @Test
    void drainsFullBatchesAndStopsOnPartialBatch() {
        var repository = mock(RefreshTokenRepository.class);
        var cutoff = NOW.minus(Duration.ofDays(1));
        when(repository.deleteExpiredBatch(cutoff, 2)).thenReturn(2, 2, 1);
        new RefreshTokenCleanupJob(repository, CLOCK, 2, 100).deleteExpiredFamilies();
        verify(repository, times(3)).deleteExpiredBatch(cutoff, 2);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void limitsDailyWorkAndSurvivesFailure() {
        var repository = mock(RefreshTokenRepository.class);
        when(repository.deleteExpiredBatch(any(), anyInt())).thenReturn(2);
        new RefreshTokenCleanupJob(repository, CLOCK, 2, 3).deleteExpiredFamilies();
        verify(repository, times(3)).deleteExpiredBatch(any(), anyInt());

        var failing = mock(RefreshTokenRepository.class);
        when(failing.deleteExpiredBatch(any(), anyInt())).thenReturn(2).thenThrow(new IllegalStateException("test failure"));
        new RefreshTokenCleanupJob(failing, CLOCK, 2, 100).deleteExpiredFamilies();
        verify(failing, times(2)).deleteExpiredBatch(any(), anyInt());
    }

    @Test
    void rejectsUnboundedOrInvalidBatchConfiguration() {
        var repository = mock(RefreshTokenRepository.class);
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshTokenCleanupJob(repository, CLOCK, 0, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshTokenCleanupJob(repository, CLOCK, 10001, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RefreshTokenCleanupJob(repository, CLOCK, 500, 0));
    }

    private RefreshToken save(String family, Instant absoluteExpiry, Instant expiry, boolean used) {
        var token = new RefreshToken();
        token.setUserId(UUID.randomUUID().toString());
        token.setFamilyId(family);
        token.setTokenHash(RefreshSessionService.hash(UUID.randomUUID().toString()));
        token.setCreatedAt(expiry.minus(Duration.ofDays(30)));
        token.setExpiresAt(expiry);
        token.setAbsoluteExpiresAt(absoluteExpiry);
        if (used) token.setUsedAt(expiry.minus(Duration.ofDays(1)));
        return tokens.saveAndFlush(token);
    }
}
