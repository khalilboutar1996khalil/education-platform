-- Levels (class years) were a Java enum, so adding one meant a release. They now live here and an
-- admin manages them from the API. Every existing "level" column keeps storing the same reference
-- (SECOND_AS, …) and gains a foreign key to this table, so no data moves.

CREATE TABLE school_levels (
    id         BIGSERIAL PRIMARY KEY,
    -- The reference stored in every level column; never changes once created
    code       VARCHAR(20)  NOT NULL,
    -- What people read, e.g. "7ᵉ année de base informatique"
    name       VARCHAR(100) NOT NULL,
    -- Display order in lists and on the dashboard
    position   INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uq_school_levels_code UNIQUE (code)
);

-- The six levels the enum had, in school order
INSERT INTO school_levels (code, name, position, active, created_at, updated_at, created_by, updated_by)
VALUES
    ('SEVENTH_BASE', '7ᵉ année de base informatique', 10, TRUE, NOW(), NOW(), 'system', 'system'),
    ('EIGHTH_BASE',  '8ᵉ année de base informatique', 20, TRUE, NOW(), NOW(), 'system', 'system'),
    ('NINTH_BASE',   '9ᵉ année de base informatique', 30, TRUE, NOW(), NOW(), 'system', 'system'),
    ('SECOND_AS',    '2ᵉ AS informatique',            40, TRUE, NOW(), NOW(), 'system', 'system'),
    ('THIRD_AS',     '3ᵉ AS informatique',            50, TRUE, NOW(), NOW(), 'system', 'system'),
    ('FOURTH_AS',    '4ᵉ AS informatique',            60, TRUE, NOW(), NOW(), 'system', 'system');

-- A level column can only hold a level that exists
ALTER TABLE users           ADD CONSTRAINT fk_users_level           FOREIGN KEY (level) REFERENCES school_levels (code);
ALTER TABLE courses         ADD CONSTRAINT fk_courses_level         FOREIGN KEY (level) REFERENCES school_levels (code);
ALTER TABLE class_codes     ADD CONSTRAINT fk_class_codes_level     FOREIGN KEY (level) REFERENCES school_levels (code);
ALTER TABLE access_requests ADD CONSTRAINT fk_access_requests_level FOREIGN KEY (level) REFERENCES school_levels (code);
ALTER TABLE resources       ADD CONSTRAINT fk_resources_level       FOREIGN KEY (level) REFERENCES school_levels (code);
ALTER TABLE announcements   ADD CONSTRAINT fk_announcements_level   FOREIGN KEY (level) REFERENCES school_levels (code);
