-- Unknown completion dates deliberately remain NULL: creation/edit dates are not completion evidence.
ALTER TABLE tb_tasks ADD COLUMN completed_at timestamp(6) with time zone;
ALTER TABLE tb_tasks ADD COLUMN is_recurring boolean NOT NULL DEFAULT false;
ALTER TABLE tb_tasks ADD COLUMN recurrence_rule text;

ALTER TABLE events ADD COLUMN completed boolean NOT NULL DEFAULT false;
ALTER TABLE events ADD COLUMN completed_at timestamp(6) with time zone;
ALTER TABLE events ADD COLUMN is_recurring boolean NOT NULL DEFAULT false;
ALTER TABLE events ADD COLUMN recurrence_rule text;

ALTER TABLE tb_note ADD COLUMN completed boolean NOT NULL DEFAULT false;
ALTER TABLE tb_note ADD COLUMN completed_at timestamp(6) with time zone;
ALTER TABLE tb_note ADD COLUMN is_recurring boolean NOT NULL DEFAULT false;
ALTER TABLE tb_note ADD COLUMN recurrence_rule text;

-- completed is the existing task status; partial indexes exclude protected recurring items.
CREATE INDEX ix_tasks_retention ON tb_tasks (completed, completed_at, id)
    WHERE is_recurring = false AND recurrence_rule IS NULL;
CREATE INDEX ix_events_retention ON events (completed, completed_at, id)
    WHERE is_recurring = false AND recurrence_rule IS NULL;
CREATE INDEX ix_notes_retention ON tb_note (completed, completed_at, id)
    WHERE is_recurring = false AND recurrence_rule IS NULL;
