package com.example.education_platform.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Student self-registration. Public, so nothing here may be trusted beyond its shape — in
 * particular there is no level field: the level comes from whichever class code is presented,
 * so a student cannot enrol themselves into a year they were not given the code for.
 */
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

        @NotBlank(message = "{classCode.required}")
        @Size(max = 40)
        String classCode) {
}
