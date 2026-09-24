package com.example.education_platform.assignment.dto.response;

import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.storage.dto.response.StoredFileResponse;
import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentDetailResponse(
        Long id,
        Long courseId,
        String courseCode,
        String title,
        String instructions,
        AssignmentType type,
        WorkMode mode,
        Instant deadline,
        BigDecimal maxPoints,
        AssignmentStatus status,
        boolean allowLate,
        boolean acceptingSubmissions,
        StoredFileResponse brief) {
}
