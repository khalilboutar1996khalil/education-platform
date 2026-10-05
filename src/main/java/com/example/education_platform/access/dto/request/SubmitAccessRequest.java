package com.example.education_platform.access.dto.request;

import com.example.education_platform.level.validation.KnownLevel;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The public form. Anyone can post this, so nothing here may be trusted beyond its shape. */
public record SubmitAccessRequest(
        @NotBlank(message = "{user.fullName.required}")
        @Size(max = 255)
        String fullName,

        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        @Size(max = 255)
        String email,

        @NotNull(message = "{user.level.required}")
        @KnownLevel
        Level level,

        @Size(max = 1000)
        String message) {
}
