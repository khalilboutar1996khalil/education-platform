package com.example.education_platform.course.dto.response;

import com.example.education_platform.user.entity.Level;
import java.util.List;

public record CourseDetailResponse(
        Long id,
        String code,
        String title,
        String description,
        Level level,
        String color,
        List<ChapterResponse> chapters) {
}
