-- One row per active session. Only a SHA-256 hash of the token is stored, so a database
-- leak cannot be replayed as a login. Rotation revokes the old row and inserts a new one.

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP(6) WITH TIME ZONE,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(45),

    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- "Revoke every session of this user" on logout-everywhere and on password change.
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id, revoked_at);
