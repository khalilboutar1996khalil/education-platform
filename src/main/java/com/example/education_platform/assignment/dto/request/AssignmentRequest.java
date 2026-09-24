package com.example.education_platform.assignment.dto.request;

import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentRequest(
        @NotBlank(message = "{assignment.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 10_000)
        String instructions,

        @NotNull(message = "{assignment.type.required}")
        AssignmentType type,

        @NotNull(message = "{assignment.mode.required}")
        WorkMode mode,

        Instant deadline,

        @NotNull(message = "{assignment.maxPoints.required}")
        @DecimalMin(value = "0.01", message = "{assignment.maxPoints.invalid}")
        BigDecimal maxPoints,

        /** Wrapper, not a primitive: Jackson refuses a record whose primitive component is absent. */
        Boolean allowLate) {

    public boolean allowLateOrDefault() {
        return Boolean.TRUE.equals(allowLate);
    }
}
