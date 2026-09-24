package com.example.education_platform.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "{reset.token.required}")
        String token,

        @NotBlank(message = "{user.password.required}")
        @Size(min = 10, max = 100, message = "{user.password.tooShort}")
        String newPassword) {
}
