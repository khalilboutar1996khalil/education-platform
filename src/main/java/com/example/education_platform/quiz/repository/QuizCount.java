package com.example.education_platform.quiz.repository;

/** "How many attempts per quiz", so a list of quizzes costs one extra query rather than one each. */
public interface QuizCount {

    Long getQuizId();

    long getTotal();
}
