package com.example.education_platform.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The slug is derived from the title on creation and never moves afterwards. */
public record BlogPostRequest(
        @NotBlank(message = "{post.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 500)
        String excerpt,

        @NotBlank(message = "{post.content.required}")
        String content,

        @NotNull(message = "{post.category.required}")
        Long categoryId) {
}
