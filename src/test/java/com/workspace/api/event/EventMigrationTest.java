package com.workspace.api.event;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Opt-in: dedicated local disposable database, never the application's database. */
@EnabledIfSystemProperty(named = "event.migration.url", matches = "jdbc:postgresql://127[.]0[.]0[.]1:[0-9]+/event_migration_test")
class EventMigrationTest {
    private static final String OWNER = "00000000-0000-0000-0000-000000000000";
    private String url() { return System.getProperty("event.migration.url"); }
    private Flyway migration(String schema, String target) {
        return Flyway.configure().dataSource(url(), "migration_test", "")
                .schemas(schema).defaultSchema(schema).target(target).load();
    }

    @Test void copiesEveryFieldBeforeDroppingTasksAndKeepsExistingEvents() throws Exception {
        String schema = "event_copy_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(url(), "migration_test", ""); var statement = connection.createStatement()) {
            migration(schema, "5").migrate();
            connection.setSchema(schema);
            statement.execute("insert into tb_tasks(id,title,notes,priority,due_date,due_time,completed,completed_at,is_recurring,recurrence_rule,user_id) values " +
                    "(901,'Dated','Details','Alta','2030-02-03','23:30',true,'2026-09-01T10:00:00Z',false,null,'"+OWNER+"')," +
                    "(902,'Undated',null,null,null,null,false,null,false,null,'"+OWNER+"')," +
                    "(903,'Recurring','Weekly','Normal','2030-02-04',null,true,null,true,'FREQ=WEEKLY','"+OWNER+"')");
            statement.execute("insert into events(title,date,start_time,end_time,source,category,user_id,kind,scheduled_task_id) values " +
                    "('Existing','2030-02-01','09:00','10:00','internal','personal','"+OWNER+"','task',900)");
            statement.execute("create temporary table expected as select * from tb_tasks");
            migration(schema, "6").migrate();
            try (var result = statement.executeQuery("select count(*) from expected t join events e on e.scheduled_task_id=t.id where " +
                    "row(e.title,e.description,e.priority,e.date,e.start_time,e.completed,e.completed_at,e.is_recurring,e.recurrence_rule,e.user_id) " +
                    "is not distinct from row(t.title,t.notes,t.priority,t.due_date,t.due_time,t.completed,t.completed_at,t.is_recurring,t.recurrence_rule,t.user_id)")) {
                result.next(); assertThat(result.getInt(1)).isEqualTo(3);
            }
            var flyway = migration(schema, "7"); flyway.migrate(); flyway.validate();
            try (var result = statement.executeQuery("select count(*) from events where kind='event'")) {
                result.next(); assertThat(result.getInt(1)).isEqualTo(4);
            }
            try (var result = statement.executeQuery("select to_regclass('tb_tasks')")) {
                result.next(); assertThat(result.getString(1)).isNull();
            }
            try (var result = statement.executeQuery("select end_time from events where title='Existing'")) {
                result.next(); assertThat(result.getString(1)).isEqualTo("10:00:00");
            }
            // Retention still removes only old completed non-recurring events.
            String cleanup;
            try (var source = getClass().getResourceAsStream("/retention/delete-batch.sql")) {
                cleanup = new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).formatted("events", "events");
            }
            try (var deletion = connection.prepareStatement(cleanup)) {
                deletion.setObject(1, java.time.OffsetDateTime.parse("2026-09-20T00:00:00Z")); deletion.setInt(2, 100);
                assertThat(deletion.executeUpdate()).isEqualTo(1);
            }
        } finally {
            try (var connection = DriverManager.getConnection(url(), "migration_test", ""); var statement = connection.createStatement()) {
                statement.execute("drop schema if exists " + schema + " cascade");
            }
        }
    }

    @Test void refusesToDropSourceWhenCopiedDataDiffers() throws Exception {
        String schema = "event_guard_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(url(), "migration_test", ""); var statement = connection.createStatement()) {
            migration(schema, "5").migrate(); connection.setSchema(schema);
            statement.execute("insert into tb_tasks(id,title,user_id) values(901,'Original','"+OWNER+"')");
            migration(schema, "6").migrate();
            statement.execute("update events set title='Different' where scheduled_task_id=901");
            assertThatThrownBy(() -> migration(schema, "7").migrate()).hasMessageContaining("verification failed");
            try (var result = statement.executeQuery("select title from tb_tasks where id=901")) {
                result.next(); assertThat(result.getString(1)).isEqualTo("Original");
            }
        } finally {
            try (var connection = DriverManager.getConnection(url(), "migration_test", ""); var statement = connection.createStatement()) {
                statement.execute("drop schema if exists " + schema + " cascade");
            }
        }
    }
}
