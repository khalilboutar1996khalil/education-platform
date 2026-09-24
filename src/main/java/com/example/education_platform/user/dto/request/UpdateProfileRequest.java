package com.example.education_platform.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Email and role are deliberately absent: a user cannot change either about themselves. */
public record UpdateProfileRequest(
        @NotBlank(message = "{user.fullName.required}")
        @Size(max = 255)
        String fullName,

        @Pattern(regexp = "fr|en", message = "{user.locale.invalid}")
        String locale,

        @NotNull
        Boolean notifyByEmail) {
}
