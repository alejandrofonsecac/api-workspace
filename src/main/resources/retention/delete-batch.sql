DELETE FROM %s WHERE id IN (
    SELECT id FROM %s
    WHERE completed = true AND completed_at <= ?
      AND is_recurring = false AND recurrence_rule IS NULL
    ORDER BY completed_at, id
    LIMIT ? FOR UPDATE SKIP LOCKED
)
