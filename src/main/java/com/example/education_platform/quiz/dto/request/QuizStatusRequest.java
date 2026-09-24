package com.example.education_platform.quiz.dto.request;

import com.example.education_platform.quiz.entity.QuizStatus;
import jakarta.validation.constraints.NotNull;

public record QuizStatusRequest(@NotNull(message = "{quiz.status.required}") QuizStatus status) {
}
