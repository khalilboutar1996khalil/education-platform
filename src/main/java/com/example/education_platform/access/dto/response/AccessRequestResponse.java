package com.example.education_platform.access.dto.response;

import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import java.time.Instant;

/** @param createdUser the account an approval produced; null while pending or once refused */
public record AccessRequestResponse(
        Long id,
        String fullName,
        String email,
        Level level,
        String message,
        AccessRequestStatus status,
        UserResponse reviewedBy,
        Instant reviewedAt,
        UserResponse createdUser,
        String decisionNote,
        Instant submittedAt) {
}
