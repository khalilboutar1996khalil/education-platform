package com.example.education_platform.assignment.dto.response;

import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.assignment.entity.WorkMode;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One card on the TP &amp; devoirs screen.
 *
 * @param mySubmissionStatus the signed-in student's own state, or null for an admin
 * @param myGrade            filled once their work has been marked
 */
public record AssignmentSummaryResponse(
        Long id,
        Long courseId,
        String courseCode,
        String title,
        AssignmentType type,
        WorkMode mode,
        Instant deadline,
        BigDecimal maxPoints,
        AssignmentStatus status,
        boolean allowLate,
        boolean acceptingSubmissions,
        SubmissionStatus mySubmissionStatus,
        BigDecimal myGrade) {
}
