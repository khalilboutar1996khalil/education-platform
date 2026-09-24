package com.example.education_platform.grade.dto.response;

import java.math.BigDecimal;

/**
 * A student's standing in one module.
 *
 * @param average    weighted, out of 20; null when the module has no marks yet rather than 0,
 *                   because "no marks" and "zero" are not the same thing
 * @param gradeCount how many marks the average is built from
 */
public record CourseAverageResponse(
        Long courseId,
        String courseCode,
        String courseTitle,
        BigDecimal average,
        int gradeCount) {
}
