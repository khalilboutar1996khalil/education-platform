package com.example.education_platform.grade.dto.response;

import com.example.education_platform.grade.entity.GradeKind;
import java.math.BigDecimal;
import java.time.Instant;

/** @param outOfTwenty the mark rescaled to /20, which is how a report card reads it */
public record GradeResponse(
        Long id,
        Long courseId,
        String courseCode,
        GradeKind kind,
        String label,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal outOfTwenty,
        BigDecimal weight,
        Instant recordedAt) {
}
