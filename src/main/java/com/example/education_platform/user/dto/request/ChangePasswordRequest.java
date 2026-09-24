package com.example.education_platform.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "{user.password.required}")
        String currentPassword,

        @NotBlank(message = "{user.password.required}")
        @Size(min = 10, max = 100, message = "{user.password.tooShort}")
        String newPassword) {
}
