package com.example.education_platform.grade.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ManualGradeRequest(
        @NotNull(message = "{grade.student.required}")
        Long studentId,

        @NotBlank(message = "{grade.label.required}")
        @Size(max = 255)
        String label,

        @NotNull(message = "{grade.required}")
        @DecimalMin(value = "0.0", message = "{grade.invalid}")
        BigDecimal score,

        @NotNull(message = "{grade.maxScore.required}")
        @DecimalMin(value = "0.01", message = "{grade.maxScore.invalid}")
        BigDecimal maxScore,

        /** Coefficient; omitted means 1. */
        @DecimalMin(value = "0.01", message = "{grade.weight.invalid}")
        BigDecimal weight) {

    public BigDecimal weightOrDefault() {
        return weight == null ? BigDecimal.ONE : weight;
    }
}
