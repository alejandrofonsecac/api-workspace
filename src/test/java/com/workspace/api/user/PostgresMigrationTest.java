package com.workspace.api.user;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Run against a disposable PostgreSQL database; never point this at application data. */
@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_URL", matches = ".+")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=${MIGRATION_TEST_URL}",
        "spring.datasource.username=${MIGRATION_TEST_USER:workspace_test}",
        "spring.datasource.password=${MIGRATION_TEST_PASSWORD:}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class PostgresMigrationTest {
    private static final String LEGACY_ID = "00000000-0000-0000-0000-000000000000";
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void freshDatabaseMigratesAndPassesHibernateValidation() {
        assertThat(jdbc.queryForObject("select enabled from tb_users where id = ?", Boolean.class, LEGACY_ID)).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(9);
    }

    @Test
    void existingDatabasePreservesAllRecordsAndScopesWeeklyFocus() throws Exception {
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
            connection.setSchema(schema);
            // Reproduce an existing installation without Flyway history.
            String baseline;
            try (var source = getClass().getResourceAsStream("/db/migration/V1__existing_workspace_schema.sql")) {
                baseline = new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            statement.execute(baseline);
            statement.execute("insert into tb_note(id,title) values ('old-note','Legacy note')");
            statement.execute("insert into tb_tasks(id,title) values (99,'Legacy task')");
            statement.execute("insert into tb_event(name) values ('Legacy event')");
            statement.execute("insert into events(title,date,start_time,end_time,source,category) " +
                    "values ('Legacy planner',current_date,'09:00','10:00','internal','work')");
            statement.execute("insert into tb_habit(name) values ('Legacy habit')");
            statement.execute("insert into tb_habit_completions(habit_id,completed_date) values (1,current_date)");
            statement.execute("insert into tb_goals(goal_name,expected_target,priority,category) " +
                    "values ('Legacy goal','One','NORMAL','WORK')");
            statement.execute("insert into tb_goal_focus(week_id,focus,message) values ('2026-W38','Legacy focus','')");
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .baselineOnMigrate(true).baselineVersion("0").load().migrate();

            for (String table : new String[]{"tb_note", "tb_event", "tb_habit", "tb_goals", "tb_goal_focus"}) {
                try (var result = statement.executeQuery("select user_id from " + table)) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo(LEGACY_ID);
                    assertThat(result.next()).isFalse();
                }
            }
            try (var result = statement.executeQuery("select count(*) from events where user_id='" + LEGACY_ID + "'")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
            try (var result = statement.executeQuery("select count(*) from tb_habit_completions")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            String otherId = UUID.randomUUID().toString();
            statement.execute("insert into tb_users(id,name,email,password_hash,role,enabled) values ('" + otherId +
                    "','Other','other@example.com','unused','USER',true)");
            statement.execute("insert into tb_goal_focus(user_id,week_id,focus,message) values ('" +
                    otherId + "','2026-W38','Other focus','')");
            try (var result = statement.executeQuery("select count(*) from tb_goal_focus where week_id='2026-W38'")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(2);
            }
        } finally {
            // Only the randomly generated test schema is removed.
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }
}
