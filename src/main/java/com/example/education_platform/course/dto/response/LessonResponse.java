package com.example.education_platform.course.dto.response;

import com.example.education_platform.course.entity.LessonType;

/** @param completed whether the signed-in student has finished it; always false for an admin */
public record LessonResponse(
        Long id,
        int position,
        String title,
        LessonType type,
        Integer durationMinutes,
        String contentUrl,
        String content,
        boolean completed) {
}
