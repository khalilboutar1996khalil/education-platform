package com.example.education_platform.quiz.dto.request;

import com.example.education_platform.quiz.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * The choices replace whatever the question had. How many are required, and how many may be
 * correct, depends on the type and is checked in the service — bean validation cannot see across
 * two fields.
 */
public record QuestionRequest(
        @NotBlank(message = "{question.text.required}")
        @Size(max = 2000)
        String text,

        @NotNull(message = "{question.type.required}")
        QuestionType type,

        @NotNull(message = "{question.points.required}")
        @DecimalMin(value = "0.0", message = "{question.points.invalid}")
        BigDecimal points,

        @Valid
        List<ChoiceRequest> choices,

        @Positive(message = "{position.invalid}")
        Integer position) {
}
