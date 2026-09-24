package com.example.education_platform.course.dto.request;

import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "{course.code.required}")
        @Size(max = 20)
        @Pattern(regexp = "[A-Z0-9-]+", message = "{course.code.invalid}")
        String code,

        @NotBlank(message = "{course.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull(message = "{course.level.required}")
        Level level,

        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "{course.color.invalid}")
        String color) {
}
