package com.example.education_platform.quiz.dto.response;

import com.example.education_platform.quiz.entity.QuizStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param submissions  attempts handed in, whether auto-scored or still awaiting marking
 * @param averageScore null until at least one attempt has been handed in
 */
public record QuizDetailResponse(
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
        boolean shuffleQuestions,
        BigDecimal totalPoints,
        long submissions,
        Double averageScore,
        List<QuestionResponse> questions) {
}
