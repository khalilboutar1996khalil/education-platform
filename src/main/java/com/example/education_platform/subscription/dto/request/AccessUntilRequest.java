package com.example.education_platform.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** @param accessUntil last day the student may sign in */
public record AccessUntilRequest(@NotNull(message = "La date est obligatoire") LocalDate accessUntil) {
}
