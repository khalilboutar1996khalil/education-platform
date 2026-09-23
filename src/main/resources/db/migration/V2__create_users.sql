-- Admins and students share one table; `role` separates them.
-- The audit columns (created_at … deleted_at) come from BaseEntity and repeat on every table.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    level           VARCHAR(20),
    status          VARCHAR(20)  NOT NULL,
    locale          VARCHAR(5)   NOT NULL DEFAULT 'fr',
    notify_by_email BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_users_email UNIQUE (email),
    -- A student always belongs to a level; an admin never does.
    CONSTRAINT ck_users_level_matches_role CHECK (
        (role = 'STUDENT' AND level IS NOT NULL)
        OR (role = 'ADMIN' AND level IS NULL)
    )
);

-- The admin student list filters on these three together.
CREATE INDEX idx_users_role_level_status ON users (role, level, status);
