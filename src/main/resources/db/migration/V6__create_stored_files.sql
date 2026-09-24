-- One row per uploaded file, for the whole platform. Deliberately holds no link back to whatever
-- the file is attached to: owners point at it, so a new kind of attachment never alters this table.
-- Nothing cascades into it either, so orphans need a sweeper rather than a foreign key.

CREATE TABLE stored_files (
    id                BIGSERIAL PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    -- Generated, opaque, and the only thing used to build a path on disk
    storage_key       VARCHAR(200) NOT NULL,
    checksum          VARCHAR(64)  NOT NULL,
    uploaded_by_id    BIGINT       NOT NULL,

    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    deleted_at        TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_stored_files_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_stored_files_size CHECK (size_bytes > 0),
    CONSTRAINT fk_stored_files_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users (id)
);

-- Spotting a re-upload of identical bytes
CREATE INDEX idx_stored_files_checksum ON stored_files (checksum);
