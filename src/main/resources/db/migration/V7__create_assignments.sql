-- TP and devoirs. One table covers both: they differ by `type` and `mode`, not by structure.
-- A pair hands in a single submission with two members, so one upload and one grade serve both.

CREATE TABLE assignments (
    id            BIGSERIAL PRIMARY KEY,
    course_id     BIGINT        NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    instructions  VARCHAR(10000),
    type          VARCHAR(20)   NOT NULL,
    mode          VARCHAR(20)   NOT NULL,
    deadline      TIMESTAMP(6) WITH TIME ZONE,
    max_points    NUMERIC(5, 2) NOT NULL DEFAULT 20,
    status        VARCHAR(20)   NOT NULL,
    allow_late    BOOLEAN       NOT NULL DEFAULT FALSE,
    brief_file_id BIGINT,

    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    deleted_at    TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_assignments_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_brief FOREIGN KEY (brief_file_id) REFERENCES stored_files (id),
    CONSTRAINT ck_assignments_max_points CHECK (max_points > 0)
);

CREATE INDEX idx_assignments_course_status ON assignments (course_id, status);

CREATE TABLE submissions (
    id            BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT      NOT NULL,
    student_id    BIGINT      NOT NULL,
    partner_id    BIGINT,
    comment       VARCHAR(2000),
    status        VARCHAR(20) NOT NULL,
    submitted_at  TIMESTAMP(6) WITH TIME ZONE,
    grade         NUMERIC(5, 2),
    feedback      VARCHAR(5000),
    graded_by_id  BIGINT,
    graded_at     TIMESTAMP(6) WITH TIME ZONE,

    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    deleted_at    TIMESTAMP(6) WITH TIME ZONE,

    -- One submission per student per assignment; a pair is one row, not two
    CONSTRAINT uk_submissions_assignment_student UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_partner FOREIGN KEY (partner_id) REFERENCES users (id),
    CONSTRAINT fk_submissions_graded_by FOREIGN KEY (graded_by_id) REFERENCES users (id),
    -- Nobody is their own partner
    CONSTRAINT ck_submissions_partner_differs CHECK (partner_id IS NULL OR partner_id <> student_id),
    CONSTRAINT ck_submissions_grade CHECK (grade IS NULL OR grade >= 0)
);

-- The grading queue: "what is waiting on me for this assignment"
CREATE INDEX idx_submissions_assignment_status ON submissions (assignment_id, status);
-- "My submissions", from either side of a pair
CREATE INDEX idx_submissions_student ON submissions (student_id);
CREATE INDEX idx_submissions_partner ON submissions (partner_id);

-- A submission carries several files, so the link is its own table rather than a column
CREATE TABLE submission_files (
    submission_id BIGINT NOT NULL,
    file_id       BIGINT NOT NULL,

    PRIMARY KEY (submission_id, file_id),
    CONSTRAINT fk_submission_files_submission FOREIGN KEY (submission_id) REFERENCES submissions (id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_files_file FOREIGN KEY (file_id) REFERENCES stored_files (id)
);
