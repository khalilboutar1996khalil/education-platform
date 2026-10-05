package com.example.education_platform.level.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param code     the reference, e.g. {@code 1AS}; stored in upper case and fixed for good
 * @param name     what people read, e.g. "1ʳᵉ année secondaire"
 * @param position display order; null puts it after the existing levels
 */
public record CreateLevelRequest(
        @NotBlank(message = "{level.code.required}")
        @Size(max = 20, message = "{level.code.invalid}")
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "{level.code.invalid}")
        String code,

        @NotBlank(message = "{level.name.required}")
        @Size(max = 100)
        String name,

        Integer position) {
}
