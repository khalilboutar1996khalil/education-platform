package com.example.education_platform.quiz.entity;

public enum AttemptStatus {
    IN_PROGRESS,
    /** Handed in, but still carrying an open question nobody has marked. */
    SUBMITTED,
    GRADED,
    /** The clock or the deadline ran out before it was handed in. */
    EXPIRED
}
