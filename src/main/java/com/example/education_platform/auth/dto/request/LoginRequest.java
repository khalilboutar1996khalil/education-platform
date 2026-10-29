package com.example.education_platform.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * @param email the email address, or the username of a student who joined through a book coupon.
 *              The field keeps its name so existing clients go on working.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password) {
}
