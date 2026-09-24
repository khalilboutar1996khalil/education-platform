-- The public "demander un accès" form. A request holds no user_id until it is approved, which is
-- why it is a table of its own rather than a users row with a PENDING status: the person has no
-- account yet, and a half-real account that cannot log in is worse than a request that plainly
-- is not one.

CREATE TABLE access_requests (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    -- Not unique: somebody refused once may reasonably ask again
    email           VARCHAR(255) NOT NULL,
    level           VARCHAR(20)  NOT NULL,
    message         VARCHAR(1000),
    status          VARCHAR(20)  NOT NULL,
    reviewed_by_id  BIGINT,
    reviewed_at     TIMESTAMP(6) WITH TIME ZONE,
    created_user_id BIGINT,
    decision_note   VARCHAR(1000),

    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_access_requests_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT fk_access_requests_created_user FOREIGN KEY (created_user_id) REFERENCES users (id),
    -- An approved request must say which account it produced; a pending one cannot have named it yet
    CONSTRAINT ck_access_requests_decision CHECK (
        (status = 'PENDING' AND reviewed_at IS NULL AND created_user_id IS NULL)
        OR (status = 'APPROVED' AND reviewed_at IS NOT NULL AND created_user_id IS NOT NULL)
        OR (status = 'REJECTED' AND reviewed_at IS NOT NULL AND created_user_id IS NULL)
    )
);

-- The admin's queue: what is still waiting on a decision
CREATE INDEX idx_access_requests_status ON access_requests (status, created_at DESC);
-- Used to stop one address filing the same request over and over
CREATE INDEX idx_access_requests_email ON access_requests (email, status);
