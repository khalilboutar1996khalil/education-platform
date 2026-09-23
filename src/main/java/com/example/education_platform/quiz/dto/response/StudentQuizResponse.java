package com.example.education_platform.quiz.dto.response;

import com.example.education_platform.quiz.entity.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** A quiz as it appears on the student's list: no questions, and never the answer key. */
public record StudentQuizResponse(
        Long id,
        Long courseId,
        String courseCode,
        String title,
        String description,
        QuizStatus status,
        Integer durationMinutes,
        Instant opensAt,
        Instant deadline,
        int maxAttempts,
        long attemptsUsed,
        int questionCount,
        BigDecimal totalPoints,
        boolean openNow) {
}
