package com.example.education_platform.user.dto.response;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.UserStatus;

/** What the API exposes about a user — deliberately without the password hash or audit columns. */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String initials,
        Role role,
        Level level,
        UserStatus status) {
}
