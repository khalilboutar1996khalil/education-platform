-- One row per mark, whatever produced it. The source is a typed foreign key rather than a
-- source_id integer, so the database still guarantees an automatic grade points at a real row.

CREATE TABLE grades (
    id              BIGSERIAL PRIMARY KEY,
    student_id      BIGINT        NOT NULL,
    course_id       BIGINT        NOT NULL,
    kind            VARCHAR(20)   NOT NULL,
    label           VARCHAR(255)  NOT NULL,
    score           NUMERIC(6, 2) NOT NULL,
    max_score       NUMERIC(6, 2) NOT NULL,
    weight          NUMERIC(4, 2) NOT NULL DEFAULT 1,
    quiz_attempt_id BIGINT,
    submission_id   BIGINT,
    recorded_by_id  BIGINT,

    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_quiz_attempt FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_submission FOREIGN KEY (submission_id) REFERENCES submissions (id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_recorded_by FOREIGN KEY (recorded_by_id) REFERENCES users (id),

    CONSTRAINT ck_grades_score CHECK (score >= 0 AND max_score > 0 AND score <= max_score),
    CONSTRAINT ck_grades_weight CHECK (weight > 0),
    -- Exactly one source for an automatic grade, and none at all for a manual one
    CONSTRAINT ck_grades_source CHECK (
        (kind = 'QUIZ' AND quiz_attempt_id IS NOT NULL AND submission_id IS NULL)
        OR (kind = 'ASSIGNMENT' AND submission_id IS NOT NULL AND quiz_attempt_id IS NULL)
        OR (kind = 'MANUAL' AND quiz_attempt_id IS NULL AND submission_id IS NULL)
    )
);

-- One grade per source: re-marking updates the row rather than adding a second mark
CREATE UNIQUE INDEX uk_grades_quiz_attempt ON grades (quiz_attempt_id) WHERE quiz_attempt_id IS NOT NULL;
CREATE UNIQUE INDEX uk_grades_submission ON grades (submission_id) WHERE submission_id IS NOT NULL;

-- The gradebook reads by module, and a student reads their own marks
CREATE INDEX idx_grades_course_student ON grades (course_id, student_id);
CREATE INDEX idx_grades_student ON grades (student_id);
