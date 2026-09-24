-- Notices to the whole section or to one module. Scope is two nullable columns where null
-- means "everyone", the same rule the library uses.

CREATE TABLE announcements (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255)  NOT NULL,
    body            VARCHAR(5000) NOT NULL,
    author_id       BIGINT        NOT NULL,
    course_id       BIGINT,
    level           VARCHAR(20),
    pinned          BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Null while a draft; students never see those
    published_at    TIMESTAMP(6) WITH TIME ZONE,
    -- Frozen at publication: the audience changes afterwards, the fact of the send does not
    recipient_count INT,

    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_announcements_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_announcements_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT ck_announcements_recipients CHECK (recipient_count IS NULL OR recipient_count >= 0)
);

-- The student's feed: published, pinned first, newest next
CREATE INDEX idx_announcements_published ON announcements (published_at DESC, pinned DESC);
CREATE INDEX idx_announcements_course ON announcements (course_id);
