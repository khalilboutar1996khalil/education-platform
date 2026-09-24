package com.example.education_platform.quiz.dto.response;

import com.example.education_platform.quiz.entity.QuestionType;
import java.math.BigDecimal;
import java.util.List;

public record AttemptQuestionResponse(
        Long id,
        int position,
        String text,
        QuestionType type,
        BigDecimal points,
        List<AttemptChoiceResponse> choices) {
}
