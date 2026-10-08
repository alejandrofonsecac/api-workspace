-- Supports the scheduled removal of refresh token families after their absolute deadline.
CREATE INDEX idx_refresh_absolute_expiry ON tb_refresh_tokens (absolute_expires_at, id);
