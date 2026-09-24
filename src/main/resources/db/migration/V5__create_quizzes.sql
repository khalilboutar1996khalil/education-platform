-- Two mirrored chains: the definition (quiz → question → choice) and the response
-- (attempt → answer → chosen choices). Scoring compares one against the other, which is why
-- the response side never copies question text or point values.

CREATE TABLE quizzes (
    id                BIGSERIAL PRIMARY KEY,
    course_id         BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       VARCHAR(2000),
    status            VARCHAR(20)  NOT NULL,
    duration_minutes  INT,
    opens_at          TIMESTAMP(6) WITH TIME ZONE,
    deadline          TIMESTAMP(6) WITH TIME ZONE,
    max_attempts      INT          NOT NULL DEFAULT 1,
    shuffle_questions BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    deleted_at        TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_quizzes_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT ck_quizzes_max_attempts CHECK (max_attempts >= 1)
);

CREATE INDEX idx_quizzes_course_status ON quizzes (course_id, status);

CREATE TABLE questions (
    id         BIGSERIAL PRIMARY KEY,
    quiz_id    BIGINT        NOT NULL,
    position   INT           NOT NULL,
    text       VARCHAR(2000) NOT NULL,
    type       VARCHAR(20)   NOT NULL,
    points     NUMERIC(5, 2) NOT NULL DEFAULT 1,

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT ck_questions_points CHECK (points >= 0)
);

CREATE INDEX idx_questions_quiz_position ON questions (quiz_id, position);

CREATE TABLE choices (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT        NOT NULL,
    position    INT           NOT NULL,
    text        VARCHAR(1000) NOT NULL,
    correct     BOOLEAN       NOT NULL,

    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_choices_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);

CREATE INDEX idx_choices_question_position ON choices (question_id, position);

CREATE TABLE quiz_attempts (
    id             BIGSERIAL PRIMARY KEY,
    quiz_id        BIGINT      NOT NULL,
    student_id     BIGINT      NOT NULL,
    attempt_number INT         NOT NULL,
    started_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    submitted_at   TIMESTAMP(6) WITH TIME ZONE,
    status         VARCHAR(20) NOT NULL,
    score          NUMERIC(6, 2),
    max_score      NUMERIC(6, 2),

    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    deleted_at     TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_quiz_attempts_quiz_student_number UNIQUE (quiz_id, student_id, attempt_number),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempts_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE
);

-- "This student's attempts at this quiz", checked before every new attempt
CREATE INDEX idx_quiz_attempts_student ON quiz_attempts (student_id, quiz_id);

CREATE TABLE answers (
    id             BIGSERIAL PRIMARY KEY,
    attempt_id     BIGINT NOT NULL,
    question_id    BIGINT NOT NULL,
    text_answer    VARCHAR(5000),
    awarded_points NUMERIC(5, 2),

    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    deleted_at     TIMESTAMP(6) WITH TIME ZONE,

    -- One answer per question per attempt; re-answering updates the row rather than adding one
    CONSTRAINT uk_answers_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);

-- How a multiple-choice answer keeps several selections
CREATE TABLE answer_choices (
    answer_id BIGINT NOT NULL,
    choice_id BIGINT NOT NULL,

    PRIMARY KEY (answer_id, choice_id),
    CONSTRAINT fk_answer_choices_answer FOREIGN KEY (answer_id) REFERENCES answers (id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_choices_choice FOREIGN KEY (choice_id) REFERENCES choices (id) ON DELETE CASCADE
);
