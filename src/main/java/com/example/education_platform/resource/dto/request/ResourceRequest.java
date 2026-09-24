package com.example.education_platform.resource.dto.request;

import com.example.education_platform.resource.entity.ResourceType;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param courseId    null scopes it to every module
 * @param level       null scopes it to every level
 * @param externalUrl required when {@code type} is LINK, refused otherwise
 */
public record ResourceRequest(
        @NotBlank(message = "{resource.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull(message = "{resource.type.required}")
        ResourceType type,

        Long courseId,

        Level level,

        @Size(max = 1000)
        String externalUrl) {
}
