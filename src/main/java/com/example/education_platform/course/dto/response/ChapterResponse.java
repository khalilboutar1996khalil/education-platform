package com.example.education_platform.course.dto.response;

import java.util.List;

public record ChapterResponse(
        Long id,
        int position,
        String title,
        String summary,
        List<LessonResponse> lessons) {
}
