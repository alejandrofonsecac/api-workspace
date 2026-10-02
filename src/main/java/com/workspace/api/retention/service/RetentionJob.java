package com.workspace.api.retention.service;

import com.workspace.api.retention.repository.RetentionRepository.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true", matchIfMissing = true)
public class RetentionJob {
    private final RetentionBatchService batches;
    private final Clock clock;
    private final int batchSize;
    private final int maxBatches;

    public RetentionJob(RetentionBatchService batches,
                        Clock clock,
                        @Value("${app.retention.batch-size:500}")
                        int batchSize,
                        @Value("${app.retention.max-batches-per-resource:100}")
                        int maxBatches) {
        if (batchSize < 1 || batchSize > 10000 || maxBatches < 1)
            throw new IllegalArgumentException("Retention requires batch-size between 1 and 10000 and a positive batch limit");
        this.batches = batches;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxBatches = maxBatches;
    }

    @Scheduled(cron = "${app.retention.cron:0 0 3 * * *}", zone = "${app.time-zone:America/Sao_Paulo}")
    public void cleanCompletedItems() {
        var cutoff = clock.instant().minus(7, ChronoUnit.DAYS);
        for (Resource resource : Resource.values()) {
            long deleted = 0;
            try {
                for (int batch = 0; batch < maxBatches; batch++) {
                    int count = batches.deleteBatch(resource, cutoff, batchSize);
                    deleted += count;
                    if (count < batchSize) break;
                }
                log.info("Retention cleanup resource={} deleted={}", resource, deleted);
            } catch (RuntimeException exception) {
                // Other resources can proceed; only the failing batch rolls back.
                log.error("Retention cleanup failed resource={} committedDeletes={}", resource, deleted, exception);
            }
        }
    }
}
