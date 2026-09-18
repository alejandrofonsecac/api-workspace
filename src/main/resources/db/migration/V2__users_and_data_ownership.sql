CREATE TABLE tb_users (
    id varchar(36) PRIMARY KEY,
    name varchar(100) NOT NULL,
    email varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role varchar(20) NOT NULL CHECK (role IN ('USER', 'USER_PLUS', 'ADMIN')),
    enabled boolean NOT NULL DEFAULT true
);

-- Preserve pre-authentication data without exposing it to newly registered accounts.
-- This account cannot log in. Reassign its rows explicitly after identifying their owner.
INSERT INTO tb_users (id, name, email, password_hash, role, enabled)
VALUES ('00000000-0000-0000-0000-000000000000', 'Legacy data (disabled)',
        'legacy@workspace.invalid', '!disabled', 'USER', false);

ALTER TABLE tb_note ADD COLUMN user_id varchar(36);
ALTER TABLE tb_tasks ADD COLUMN user_id varchar(36);
ALTER TABLE tb_event ADD COLUMN user_id varchar(36);
ALTER TABLE events ADD COLUMN user_id varchar(36);
ALTER TABLE tb_habit ADD COLUMN user_id varchar(36);
ALTER TABLE tb_goals ADD COLUMN user_id varchar(36);
ALTER TABLE tb_goal_focus ADD COLUMN user_id varchar(36);

UPDATE tb_note SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE tb_tasks SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE tb_event SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE events SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE tb_habit SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE tb_goals SET user_id = '00000000-0000-0000-0000-000000000000';
UPDATE tb_goal_focus SET user_id = '00000000-0000-0000-0000-000000000000';

ALTER TABLE tb_note ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tb_tasks ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tb_event ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE events ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tb_habit ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tb_goals ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tb_goal_focus ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE tb_note ADD CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE tb_tasks ADD CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE tb_event ADD CONSTRAINT fk_schedule_event_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE events ADD CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE tb_habit ADD CONSTRAINT fk_habit_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE tb_goals ADD CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES tb_users(id);
ALTER TABLE tb_goal_focus ADD CONSTRAINT fk_focus_user FOREIGN KEY (user_id) REFERENCES tb_users(id);

-- The old primary key allowed just one focus per week across the entire application.
DO $$
DECLARE primary_key_name text;
BEGIN
    SELECT conname INTO primary_key_name FROM pg_constraint
        WHERE conrelid = 'tb_goal_focus'::regclass AND contype = 'p';
    EXECUTE format('ALTER TABLE tb_goal_focus DROP CONSTRAINT %I', primary_key_name);
END $$;
ALTER TABLE tb_goal_focus ADD PRIMARY KEY (user_id, week_id);

CREATE INDEX ix_note_user ON tb_note(user_id);
CREATE INDEX ix_tasks_user_due ON tb_tasks(user_id, due_date);
CREATE INDEX ix_schedule_event_user ON tb_event(user_id);
CREATE INDEX ix_events_user_date_time ON events(user_id, date, start_time);
CREATE INDEX ix_habit_user ON tb_habit(user_id);
CREATE INDEX ix_goals_user_week ON tb_goals(user_id, week_id);
