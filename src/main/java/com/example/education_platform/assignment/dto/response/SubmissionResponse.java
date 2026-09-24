package com.example.education_platform.assignment.dto.response;

import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.storage.dto.response.StoredFileResponse;
import com.example.education_platform.user.dto.response.UserResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** @param partner null unless the assignment is pair work and a partner was named */
public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        UserResponse student,
        UserResponse partner,
        String comment,
        SubmissionStatus status,
        Instant submittedAt,
        BigDecimal grade,
        String feedback,
        Instant gradedAt,
        List<StoredFileResponse> files) {
}
