package com.example.education_platform.level.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The reference cannot change: every student, module and code points at it. */
public record UpdateLevelRequest(
        @NotBlank(message = "{level.name.required}")
        @Size(max = 100)
        String name,

        @NotNull
        Integer position) {
}
