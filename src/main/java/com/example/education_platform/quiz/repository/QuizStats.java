package com.example.education_platform.quiz.repository;

/** @param averageScore null until at least one attempt has been handed in */
public interface QuizStats {

    long getSubmissions();

    Double getAverageScore();
}
