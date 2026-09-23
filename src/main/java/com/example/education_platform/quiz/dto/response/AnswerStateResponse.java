package com.example.education_platform.quiz.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** @param awardedPoints null until the attempt has been submitted and scored */
public record AnswerStateResponse(
        Long questionId,
        List<Long> selectedChoiceIds,
        String textAnswer,
        BigDecimal awardedPoints) {
}
