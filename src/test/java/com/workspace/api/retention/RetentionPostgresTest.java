package com.workspace.api.retention;

import com.workspace.api.retention.repository.RetentionRepository.Resource;
import com.workspace.api.retention.service.RetentionBatchService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** Only an explicitly supplied disposable local database may run this suite. */
@EnabledIfSystemProperty(named = "retention.test.url", matches = "jdbc:postgresql://127[.]0[.]0[.]1:[0-9]+/retention_test")
@ActiveProfiles({"test", "retention-integration"})
@SpringBootTest(classes = RetentionTestConfiguration.class, properties = {
        "spring.datasource.url=${retention.test.url}",
        "spring.datasource.username=retention_test",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class RetentionPostgresTest {
    private static final String OWNER = "00000000-0000-0000-0000-000000000000";
    private static final Instant CUTOFF = Instant.parse("2026-09-20T06:00:00Z");
    @Autowired RetentionBatchService batches;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void clearTestRecords() {
        for (Resource resource : Resource.values()) jdbc.update("delete from " + table(resource));
    }

    private String table(Resource resource) {
        return switch (resource) {
            case TASKS -> "tb_tasks";
            case EVENTS -> "events";
            case NOTES -> "tb_note";
        };
    }

    private Object id(Resource resource, long number) { return resource == Resource.NOTES ? "note-" + number : number; }

    private void insert(Resource resource, long number, boolean completed, Instant time, boolean recurring, String rule) {
        String extraColumns = resource == Resource.EVENTS ? ",date,start_time,end_time,source,category" : "";
        String extraValues = resource == Resource.EVENTS ? ",'2020-01-01','09:00','10:00','internal','personal'" : "";
        jdbc.update("insert into " + table(resource) + "(id,user_id,title,completed,completed_at,is_recurring,recurrence_rule" + extraColumns + ") values (?,?,?,?,?,?,?" + extraValues + ")",
                id(resource, number), OWNER, "Test", completed, time == null ? null : time.atOffset(ZoneOffset.UTC), recurring, rule);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void removesOnlyEligibleItemsAtTheExactBoundaryInBoundedBatches(Resource resource) {
        insert(resource, 1, true, CUTOFF.minusSeconds(1), false, null);
        insert(resource, 2, true, CUTOFF, false, null);
        insert(resource, 3, true, CUTOFF.plusNanos(1000), false, null);
        insert(resource, 4, false, CUTOFF.minusSeconds(1), false, null);
        insert(resource, 5, true, null, false, null);
        insert(resource, 6, true, CUTOFF, true, null);
        insert(resource, 7, true, CUTOFF, false, "FREQ=DAILY");
        insert(resource, 8, true, CUTOFF, true, "FREQ=WEEKLY");
        assertThat(batches.deleteBatch(resource, CUTOFF, 1)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from " + table(resource), Integer.class)).isEqualTo(7);
        assertThat(batches.deleteBatch(resource, CUTOFF, 1)).isEqualTo(1);
        assertThat(batches.deleteBatch(resource, CUTOFF, 1)).isZero();
        assertThat(jdbc.queryForList("select id from " + table(resource), Object.class))
                .containsExactlyInAnyOrder(id(resource, 3), id(resource, 4), id(resource, 5), id(resource, 6), id(resource, 7), id(resource, 8));
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void skipsLockedRowsAndRespectsConcurrentReopening(Resource resource) throws Exception {
        insert(resource, 1, true, CUTOFF, false, null);
        insert(resource, 2, true, CUTOFF, false, null);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("select id from " + table(resource) + " where id = ? for update")) {
                    statement.setObject(1, id(resource, 1));
                    statement.executeQuery().close();
                }
                assertThat(batches.deleteBatch(resource, CUTOFF, 10)).isEqualTo(1);
                try (var statement = connection.prepareStatement("update " + table(resource) + " set completed=false, completed_at=null where id=?")) {
                    statement.setObject(1, id(resource, 1));
                    statement.executeUpdate();
                }
                connection.commit();
            } finally {
                connection.rollback();
            }
        }
        assertThat(batches.deleteBatch(resource, CUTOFF, 10)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from " + table(resource), Integer.class)).isEqualTo(1);
    }

    @Test
    void batchesCommitIndependentlyOfCallerRollback() {
        insert(Resource.TASKS, 1, true, CUTOFF, false, null);
        insert(Resource.TASKS, 2, true, CUTOFF, false, null);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertThat(batches.deleteBatch(Resource.TASKS, CUTOFF, 1)).isEqualTo(1);
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from tb_tasks", Integer.class)).isEqualTo(1);
    }

    @Test
    void migrationPreservesUnknownCompletionDatesAndBuildsPartialIndexes() throws Exception {
        String schema = "retention_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("4").load().migrate();
            jdbc.update("insert into " + schema + ".tb_tasks(id,user_id,title,completed) values (1,?,'Legacy',true)", OWNER);
            Flyway flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
            flyway.migrate();
            flyway.validate();
            assertThat(jdbc.queryForObject("select completed from " + schema + ".tb_tasks where id=1", Boolean.class)).isTrue();
            assertThat(jdbc.queryForObject("select completed_at from " + schema + ".tb_tasks where id=1", Object.class)).isNull();
            assertThat(jdbc.queryForList("select indexdef from pg_indexes where schemaname=? and indexname like '%_retention'", String.class, schema))
                    .hasSize(3).allSatisfy(definition -> assertThat(definition).contains("completed, completed_at, id", "is_recurring = false", "recurrence_rule IS NULL"));
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
