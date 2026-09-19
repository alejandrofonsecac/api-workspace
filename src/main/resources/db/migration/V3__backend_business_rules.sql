-- Additive migration for the existing multi-user workspace schema (V1 and V2).
-- Recover the original V1/V2 files before deploying to an installation that uses them.
ALTER TABLE events ADD COLUMN IF NOT EXISTS kind varchar(255) NOT NULL DEFAULT 'event';
ALTER TABLE events ADD COLUMN IF NOT EXISTS scheduled_task_id bigint;
CREATE UNIQUE INDEX IF NOT EXISTS ux_events_scheduled_task ON events(scheduled_task_id);

ALTER TABLE tb_tasks ADD COLUMN IF NOT EXISTS due_time time(6);

ALTER TABLE tb_habit ADD COLUMN IF NOT EXISTS icon varchar(255) NOT NULL DEFAULT 'self_improvement';
ALTER TABLE tb_habit ADD COLUMN IF NOT EXISTS frequency_type varchar(255) NOT NULL DEFAULT 'daily';
ALTER TABLE tb_habit ADD COLUMN IF NOT EXISTS status varchar(255) NOT NULL DEFAULT 'active';
ALTER TABLE tb_habit ADD COLUMN IF NOT EXISTS time time(6);
ALTER TABLE tb_habit ADD COLUMN IF NOT EXISTS created_at date;
-- Retain existing completion history when establishing the earliest known date.
UPDATE tb_habit h SET created_at = COALESCE(
    (SELECT MIN(c.completed_date) FROM tb_habit_completions c WHERE c.habit_id = h.id),
    h.local_date, CURRENT_DATE) WHERE h.created_at IS NULL;
ALTER TABLE tb_habit ALTER COLUMN created_at SET NOT NULL;
CREATE TABLE IF NOT EXISTS tb_habit_weekdays (
    habit_id integer NOT NULL REFERENCES tb_habit(id),
    day_of_week integer NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    PRIMARY KEY (habit_id, day_of_week)
);
UPDATE tb_habit SET frequency_type = 'custom' WHERE frequency_every_day = false AND local_date IS NOT NULL;
INSERT INTO tb_habit_weekdays(habit_id, day_of_week)
SELECT id, EXTRACT(ISODOW FROM local_date)::integer FROM tb_habit
WHERE frequency_type = 'custom' AND local_date IS NOT NULL
ON CONFLICT DO NOTHING;
