package com.example.education_platform.assignment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GradeRequest(
        @NotNull(message = "{grade.required}")
        @DecimalMin(value = "0.0", message = "{grade.invalid}")
        BigDecimal grade,

        @Size(max = 5000)
        String feedback) {
}
