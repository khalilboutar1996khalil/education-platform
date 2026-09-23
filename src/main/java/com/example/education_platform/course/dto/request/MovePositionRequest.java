package com.example.education_platform.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovePositionRequest(
        @NotNull(message = "{position.invalid}")
        @Positive(message = "{position.invalid}")
        Integer position) {
}
