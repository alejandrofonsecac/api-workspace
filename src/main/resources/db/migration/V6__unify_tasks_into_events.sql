-- Copy first, verify every source row, then remove the obsolete table in this transaction.
ALTER TABLE events ADD COLUMN priority varchar(255);
ALTER TABLE events ALTER COLUMN date DROP NOT NULL;
ALTER TABLE events ALTER COLUMN start_time DROP NOT NULL;
ALTER TABLE events ALTER COLUMN end_time DROP NOT NULL;

-- scheduled_task_id is a temporary provenance key for verification, never a reused event ID.
INSERT INTO events (title, description, priority, date, start_time, end_time,
                    source, category, user_id, kind, completed, completed_at,
                    is_recurring, recurrence_rule, scheduled_task_id)
SELECT COALESCE(t.title, ''), t.notes, t.priority, t.due_date,
       CASE WHEN t.due_date IS NOT NULL THEN t.due_time ELSE NULL END, NULL,
       'internal', 'personal', t.user_id, 'event', t.completed, t.completed_at,
       t.is_recurring, t.recurrence_rule, t.id
FROM tb_tasks t
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.scheduled_task_id = t.id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tb_tasks t WHERE NOT EXISTS
        (SELECT 1 FROM events e WHERE e.scheduled_task_id = t.id AND e.user_id = t.user_id)) THEN
        RAISE EXCEPTION 'Task migration verification failed; source data retained';
    END IF;
END $$;

UPDATE events SET kind = 'event';
