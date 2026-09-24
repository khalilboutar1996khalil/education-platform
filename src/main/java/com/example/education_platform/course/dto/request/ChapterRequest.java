package com.example.education_platform.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** @param position 1-based; null appends to the end */
public record ChapterRequest(
        @NotBlank(message = "{chapter.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String summary,

        @Positive(message = "{position.invalid}")
        Integer position) {
}
