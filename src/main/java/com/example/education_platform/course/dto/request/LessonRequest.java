package com.example.education_platform.course.dto.request;

import com.example.education_platform.course.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** @param position 1-based; null appends to the end */
public record LessonRequest(
        @NotBlank(message = "{lesson.title.required}")
        @Size(max = 255)
        String title,

        @NotNull(message = "{lesson.type.required}")
        LessonType type,

        @Positive(message = "{lesson.duration.invalid}")
        Integer durationMinutes,

        @Size(max = 255)
        String contentUrl,

        @Size(max = 10_000)
        String content,

        @Positive(message = "{position.invalid}")
        Integer position) {
}
