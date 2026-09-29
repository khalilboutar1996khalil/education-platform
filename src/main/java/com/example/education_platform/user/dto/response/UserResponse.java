package com.example.education_platform.user.dto.response;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.UserStatus;

/**
 * What the API exposes about a user — deliberately without the password hash or audit columns.
 * The preferences are included because PATCH /me requires them: a client cannot send back a
 * value it was never shown without overwriting it.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String initials,
        Role role,
        Level level,
        UserStatus status,
        String locale,
        boolean notifyByEmail) {
}
