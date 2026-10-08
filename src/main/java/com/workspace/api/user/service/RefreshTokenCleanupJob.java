package com.workspace.api.user.service;

import com.workspace.api.user.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Removes refresh token families after their absolute deadline. Used and revoked hashes of
 * families that can still refresh are kept, because they are needed to detect replay.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.refresh.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class RefreshTokenCleanupJob {
    // Margin for clock skew between API instances; an expired family is already rejected by refresh.
    static final Duration GRACE = Duration.ofDays(1);

    private final RefreshTokenRepository tokens;
    private final Clock clock;
    private final int batchSize;
    private final int maxBatches;

    public RefreshTokenCleanupJob(RefreshTokenRepository tokens,
                                  Clock clock,
                                  @Value("${app.refresh.cleanup.batch-size:500}")
                                  int batchSize,
                                  @Value("${app.refresh.cleanup.max-batches:100}")
                                  int maxBatches) {
        if (batchSize < 1 || batchSize > 10000 || maxBatches < 1)
            throw new IllegalArgumentException("Refresh cleanup requires batch-size between 1 and 10000 and a positive batch limit");
        this.tokens = tokens;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxBatches = maxBatches;
    }

    @Scheduled(cron = "${app.refresh.cleanup.cron:0 30 3 * * *}", zone = "${app.time-zone:America/Sao_Paulo}")
    public void deleteExpiredFamilies() {
        var cutoff = clock.instant().minus(GRACE);
        long deleted = 0;
        try {
            for (int batch = 0; batch < maxBatches; batch++) {
                int count = tokens.deleteExpiredBatch(cutoff, batchSize);
                deleted += count;
                if (count < batchSize) break;
            }
            log.info("Refresh token cleanup deleted={}", deleted);
        } catch (RuntimeException exception) {
            // Committed batches stay deleted; the next run continues from the remaining rows.
            log.error("Refresh token cleanup failed committedDeletes={}", deleted, exception);
        }
    }
}
