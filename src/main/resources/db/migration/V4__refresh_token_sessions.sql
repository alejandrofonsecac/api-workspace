CREATE TABLE tb_refresh_tokens (
    id varchar(36) PRIMARY KEY,
    user_id varchar(36) NOT NULL REFERENCES tb_users(id),
    family_id varchar(36) NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    absolute_expires_at timestamptz NOT NULL,
    used_at timestamptz,
    revoked_at timestamptz,
    CONSTRAINT refresh_expiry_order CHECK (created_at < expires_at AND expires_at <= absolute_expires_at)
);
CREATE INDEX idx_refresh_user ON tb_refresh_tokens(user_id);
CREATE INDEX idx_refresh_family ON tb_refresh_tokens(family_id);
-- Retain consumed hashes until the family's absolute expiry to detect replay.
