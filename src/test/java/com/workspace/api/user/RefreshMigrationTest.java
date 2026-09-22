package com.workspace.api.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** Only an explicitly provided disposable localhost PostgreSQL is accepted. */
@EnabledIfEnvironmentVariable(named = "REFRESH_TEST_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/postgres")
class RefreshMigrationTest {
    @Test
    void migrationPreservesUsersAndEnforcesTokenConstraints() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("REFRESH_TEST_URL"), "refresh_test", "")) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                String schema = "refresh_test_" + UUID.randomUUID().toString().replace("-", "");
                sql.execute("create schema " + schema);
                sql.execute("set search_path to " + schema);
                sql.execute("create table tb_users(id varchar(36) primary key)");
                sql.execute("insert into tb_users values ('existing-user')");
                try (var source = getClass().getResourceAsStream("/db/migration/V4__refresh_token_sessions.sql")) {
                    assertThat(source).isNotNull();
                    sql.execute(new String(source.readAllBytes(), StandardCharsets.UTF_8));
                }
                sql.execute("insert into tb_refresh_tokens(id,user_id,family_id,token_hash,created_at,expires_at,absolute_expires_at) "
                        + "values ('token-id','existing-user','family',repeat('a',64),now(),now()+interval '30 days',now()+interval '90 days')");
                try (var rows = sql.executeQuery("select count(*) from tb_users")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
                for (String invalid : new String[]{
                        "update tb_refresh_tokens set user_id='missing'",
                        "update tb_refresh_tokens set expires_at=created_at",
                        "update tb_refresh_tokens set expires_at=absolute_expires_at+interval '1 day'",
                        "insert into tb_refresh_tokens select 'duplicate',user_id,family_id,token_hash,created_at,expires_at,absolute_expires_at,used_at,revoked_at from tb_refresh_tokens"
                }) {
                    var point = connection.setSavepoint();
                    org.assertj.core.api.Assertions.assertThatThrownBy(() -> sql.execute(invalid))
                            .isInstanceOf(java.sql.SQLException.class);
                    connection.rollback(point);
                }
            } finally {
                connection.rollback();
            }
        }
    }
}
