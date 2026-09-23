package com.example.education_platform.quiz.dto.response;

import com.example.education_platform.quiz.entity.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Deliberately without submission stats: those cost a query per quiz and belong on the detail. */
public record QuizSummaryResponse(
        Long id,
        Long courseId,
        String courseCode,
        String title,
        QuizStatus status,
        Integer durationMinutes,
        Instant opensAt,
        Instant deadline,
        int maxAttempts,
        int questionCount,
        BigDecimal totalPoints) {
}
