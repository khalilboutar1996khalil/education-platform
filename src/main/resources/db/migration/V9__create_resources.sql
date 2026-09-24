-- The library. Scope is two nullable columns where null means "everyone", so a resource with
-- neither a course nor a level is visible to the whole section.

CREATE TABLE resources (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    description    VARCHAR(1000),
    type           VARCHAR(20)  NOT NULL,
    course_id      BIGINT,
    level          VARCHAR(20),
    file_id        BIGINT,
    external_url   VARCHAR(1000),
    download_count BIGINT       NOT NULL DEFAULT 0,

    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    deleted_at     TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_resources_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_resources_file FOREIGN KEY (file_id) REFERENCES stored_files (id),
    -- Either an upload or a link, never both and never neither
    CONSTRAINT ck_resources_source CHECK (
        (type = 'LINK' AND external_url IS NOT NULL AND file_id IS NULL)
        OR (type <> 'LINK' AND file_id IS NOT NULL AND external_url IS NULL)
    ),
    CONSTRAINT ck_resources_download_count CHECK (download_count >= 0)
);

CREATE INDEX idx_resources_course ON resources (course_id);
CREATE INDEX idx_resources_level_type ON resources (level, type);
