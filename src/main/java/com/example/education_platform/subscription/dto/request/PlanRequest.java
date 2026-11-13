package com.example.education_platform.subscription.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** @param durationDays left out means a year (365 days) */
public record PlanRequest(
        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0", message = "Le prix ne peut pas être négatif")
        @Digits(integer = 7, fraction = 3, message = "Prix invalide")
        BigDecimal price,

        @Min(value = 1, message = "La durée doit être d'au moins un jour")
        @Max(value = 1000, message = "La durée ne peut pas dépasser 1000 jours")
        Integer durationDays,

        boolean active) {
}
