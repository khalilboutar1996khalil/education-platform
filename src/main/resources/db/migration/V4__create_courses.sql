-- Curriculum: a module holds ordered chapters, each holding ordered lessons.
-- Progress is never stored; it is counted from lesson_completions.
-- Note: quiz_id / assignment_id are added to lessons by later migrations, once those tables exist.

CREATE TABLE courses (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)   NOT NULL,
    title       VARCHAR(255)  NOT NULL,
    description VARCHAR(2000),
    level       VARCHAR(20)   NOT NULL,
    color       VARCHAR(7),

    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_courses_code UNIQUE (code)
);

CREATE INDEX idx_courses_level ON courses (level);

CREATE TABLE chapters (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT       NOT NULL,
    position   INT          NOT NULL,
    title      VARCHAR(255) NOT NULL,
    summary    VARCHAR(1000),

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_chapters_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE INDEX idx_chapters_course_position ON chapters (course_id, position);

CREATE TABLE lessons (
    id               BIGSERIAL PRIMARY KEY,
    chapter_id       BIGINT       NOT NULL,
    position         INT          NOT NULL,
    title            VARCHAR(255) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    duration_minutes INT,
    content_url      VARCHAR(255),
    content          VARCHAR(10000),

    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    deleted_at       TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_lessons_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE
);

CREATE INDEX idx_lessons_chapter_position ON lessons (chapter_id, position);

CREATE TABLE lesson_completions (
    id         BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    lesson_id  BIGINT NOT NULL,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    -- Marking a lesson done twice is idempotent rather than a second row
    CONSTRAINT uk_lesson_completions_student_lesson UNIQUE (student_id, lesson_id),
    CONSTRAINT fk_lesson_completions_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_completions_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
);

-- "What has this student finished in this module", the query behind every progress bar
CREATE INDEX idx_lesson_completions_student ON lesson_completions (student_id, lesson_id);
