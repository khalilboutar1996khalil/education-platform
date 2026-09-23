package com.example.education_platform.user.dto.response;

/**
 * The temporary password is returned exactly once, because no mail is sent yet — the admin
 * passes it to the student by hand. Step 9 replaces this with an invitation email, and this
 * field disappears with it.
 */
public record InviteStudentResponse(UserResponse user, String temporaryPassword) {
}
