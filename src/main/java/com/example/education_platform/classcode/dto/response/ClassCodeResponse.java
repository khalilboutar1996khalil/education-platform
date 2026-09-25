package com.example.education_platform.classcode.dto.response;

import com.example.education_platform.user.entity.Level;

/** Admin-only: the code itself is the secret students present, so this never leaves an admin route. */
public record ClassCodeResponse(
        Long id,
        String code,
        Level level,
        String label,
        boolean active) {
}
