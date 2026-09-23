package com.example.education_platform.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChoiceRequest(
        @NotBlank(message = "{choice.text.required}")
        @Size(max = 1000)
        String text,

        @NotNull(message = "{choice.correct.required}")
        Boolean correct) {
}
