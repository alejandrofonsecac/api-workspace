-- V6 copies Task records. Verify mapped values again before retiring the source.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tb_tasks t LEFT JOIN events e ON e.scheduled_task_id = t.id
        WHERE e.id IS NULL OR ROW(e.title, e.description, e.priority, e.date, e.start_time,
            e.completed, e.completed_at, e.is_recurring, e.recurrence_rule, e.user_id)
        IS DISTINCT FROM ROW(COALESCE(t.title, ''), t.notes, t.priority, t.due_date,
            CASE WHEN t.due_date IS NOT NULL THEN t.due_time ELSE NULL END,
            t.completed, t.completed_at, t.is_recurring, t.recurrence_rule, t.user_id)) THEN
        RAISE EXCEPTION 'Task migration verification failed; source table retained';
    END IF;
END $$;
ALTER TABLE events DROP COLUMN scheduled_task_id;
DROP TABLE tb_tasks;
DROP SEQUENCE IF EXISTS tb_tasks_seq;
