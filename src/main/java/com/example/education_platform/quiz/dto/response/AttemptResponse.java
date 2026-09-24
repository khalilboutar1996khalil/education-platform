package com.example.education_platform.quiz.dto.response;

import com.example.education_platform.quiz.entity.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param expiresAt when the clock runs out, or null for an untimed quiz
 * @param score     null until submitted
 */
public record AttemptResponse(
        Long id,
        Long quizId,
        String quizTitle,
        int attemptNumber,
        AttemptStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        List<AttemptQuestionResponse> questions,
        List<AnswerStateResponse> answers) {
}
