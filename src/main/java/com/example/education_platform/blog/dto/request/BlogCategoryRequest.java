package com.example.education_platform.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlogCategoryRequest(
        @NotBlank(message = "{category.name.required}")
        @Size(max = 255)
        String name,

        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "{category.color.invalid}")
        String color) {
}
