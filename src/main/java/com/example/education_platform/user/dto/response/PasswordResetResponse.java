package com.example.education_platform.user.dto.response;

/**
 * Returned exactly once: the application sends no email, so the admin hands the new password to
 * the student in person.
 */
public record PasswordResetResponse(UserResponse user, String temporaryPassword) {
}
