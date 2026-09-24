-- Two different things. A notification is addressed to one person and gets read; an activity
-- event is an append-only log the dashboard aggregates.

CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT       NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         VARCHAR(1000),
    link         VARCHAR(500),
    -- Null means unread; the badge counts these
    read_at      TIMESTAMP(6) WITH TIME ZONE,

    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    deleted_at   TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
);

-- The unread badge runs on every page load
CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, read_at);

CREATE TABLE activity_events (
    id           BIGSERIAL PRIMARY KEY,
    actor_id     BIGINT,
    type         VARCHAR(40)  NOT NULL,
    summary      VARCHAR(500) NOT NULL,
    subject_type VARCHAR(40),
    -- Deliberately not a foreign key: the log must outlive whatever it describes
    subject_id   BIGINT,
    course_id    BIGINT,
    occurred_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    deleted_at   TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_activity_events_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_events_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_events_occurred ON activity_events (occurred_at DESC);
CREATE INDEX idx_activity_events_course ON activity_events (course_id, occurred_at DESC);
