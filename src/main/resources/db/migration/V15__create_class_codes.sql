-- Self-registration replaces waiting for an admin to approve an access request, but the platform
-- is not public: some modules are paid, so anyone who signs up must prove they belong to a class.
-- A class code is the lightest thing that does that — the teacher hands it out in class, the
-- student types it once, and no email round trip is involved.
--
-- The code also decides the level, which is why level is not a field on the registration form:
-- letting the student pick would let a 2ᵉ AS code open the 3ᵉ AS material.

CREATE TABLE class_codes (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(40)  NOT NULL,
    level      VARCHAR(20)  NOT NULL,
    label      VARCHAR(255),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    -- Codes are compared case-insensitively, so uniqueness has to be too
    CONSTRAINT uq_class_codes_code UNIQUE (code)
);

-- The registration lookup: by code, active only
CREATE INDEX idx_class_codes_active ON class_codes (code, active);

-- One starting code per level so a fresh install can register somebody immediately. Rotate them
-- from the admin screen at the start of each year rather than editing this migration.
INSERT INTO class_codes (code, level, label, active, created_at, updated_at, created_by, updated_by)
VALUES
    ('INF2AS-2026', 'SECOND_AS', '2ᵉ AS informatique — 2026', TRUE, NOW(), NOW(), 'system', 'system'),
    ('INF3AS-2026', 'THIRD_AS',  '3ᵉ AS informatique — 2026', TRUE, NOW(), NOW(), 'system', 'system');
