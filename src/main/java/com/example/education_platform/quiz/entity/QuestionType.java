package com.example.education_platform.quiz.entity;

public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    /** Free text: scored by hand, so an attempt containing one is never fully auto-graded. */
    OPEN;

    public boolean isAutoGradable() {
        return this != OPEN;
    }
}
