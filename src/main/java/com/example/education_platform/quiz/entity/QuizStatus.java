package com.example.education_platform.quiz.entity;

public enum QuizStatus {
    /** Being written; invisible to students. */
    DRAFT,
    /** Open for attempts. */
    IN_PROGRESS,
    /** No further attempts accepted. */
    CLOSED
}
