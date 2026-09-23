package com.example.education_platform.course.dto.response;

import com.example.education_platform.user.entity.Level;

/**
 * One card on the Modules screen.
 *
 * @param progressPercent     the signed-in student's own progress; null when an admin is looking
 * @param classAveragePercent average progress of the active students at this level; null for a student
 */
public record CourseSummaryResponse(
        Long id,
        String code,
        String title,
        String description,
        Level level,
        String color,
        long chapterCount,
        long lessonCount,
        Integer progressPercent,
        Integer classAveragePercent) {
}
