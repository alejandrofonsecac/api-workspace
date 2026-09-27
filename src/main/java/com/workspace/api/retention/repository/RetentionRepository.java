package com.workspace.api.retention.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
public class RetentionRepository {
    public enum Resource {
        TASKS("tb_tasks"), EVENTS("events"), NOTES("tb_note");

        private final String table;
        Resource(String table) { this.table = table; }
    }

    private static final String DELETE_BATCH = loadStatement();

    private final JdbcTemplate jdbc;

    public int deleteBatch(Resource resource, Instant cutoff, int batchSize) {
        return jdbc.update(DELETE_BATCH.formatted(resource.table, resource.table),
                cutoff.atOffset(ZoneOffset.UTC), batchSize);
    }

    private static String loadStatement() {
        try (var input = new ClassPathResource("retention/delete-batch.sql").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
