package com.example.education_platform.auth.dto.request;

import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Student self-registration. Public: the student picks their own level. */
public record RegisterRequest(
        @NotBlank(message = "{user.fullName.required}")
        @Size(max = 255)
        String fullName,

        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        @Size(max = 255)
        String email,

        @NotBlank(message = "{user.password.required}")
        @Size(min = 10, max = 100, message = "{user.password.tooShort}")
        String password,

        @NotNull(message = "{user.level.required}")
        Level level) {
}
