package com.workspace.api.planner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Uses a disposable local PostgreSQL database, independent of application credentials. */
@EnabledIfEnvironmentVariable(named = "RULES_MIGRATION_TEST_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.+")
class BusinessRulesMigrationTest {
    @Test
    void additiveMigrationPreservesRecordsAndBackfillsHabitHistory() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("RULES_MIGRATION_TEST_URL"), "rules_test", "")) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                String schema = "rules_" + UUID.randomUUID().toString().replace("-", "");
                sql.execute("create schema " + schema);
                sql.execute("set search_path to " + schema);
                sql.execute("create table events(id bigint primary key, title varchar(255))");
                sql.execute("create table tb_tasks(id bigint primary key, title varchar(255))");
                sql.execute("create table tb_habit(id integer primary key, name varchar(255), frequency_every_day boolean, local_date date)");
                sql.execute("create table tb_habit_completions(habit_id integer, completed_date date)");
                sql.execute("insert into events values(1, 'Keep event')");
                sql.execute("insert into tb_tasks values(1, 'Keep task')");
                sql.execute("insert into tb_habit values(1, 'Keep habit', false, '2026-09-14')");
                sql.execute("insert into tb_habit_completions values(1, '2026-09-07')");
                try (var source = getClass().getResourceAsStream("/db/migration/V3__backend_business_rules.sql")) {
                    assertThat(source).isNotNull();
                    sql.execute(new String(source.readAllBytes(), StandardCharsets.UTF_8));
                }
                try (var result = sql.executeQuery("select h.name, h.created_at, h.frequency_type, w.day_of_week from tb_habit h join tb_habit_weekdays w on w.habit_id=h.id")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("Keep habit");
                    assertThat(result.getString(2)).isEqualTo("2026-09-07");
                    assertThat(result.getString(3)).isEqualTo("custom");
                    assertThat(result.getInt(4)).isEqualTo(1);
                }
                try (var result = sql.executeQuery("select title, kind from events")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("Keep event");
                    assertThat(result.getString(2)).isEqualTo("event");
                }
            } finally {
                // DDL and data are rolled back together, leaving the disposable DB unchanged.
                connection.rollback();
            }
        }
    }
}
