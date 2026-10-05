package com.example.education_platform.announcement.dto.request;

import com.example.education_platform.level.validation.KnownLevel;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param courseId null addresses the whole section rather than one module
 * @param level    null addresses every level
 */
public record AnnouncementRequest(
        @NotBlank(message = "{announcement.title.required}")
        @Size(max = 255)
        String title,

        @NotBlank(message = "{announcement.body.required}")
        @Size(max = 5000)
        String body,

        Long courseId,

        @KnownLevel
        Level level,

        Boolean pinned) {

    public boolean pinnedOrDefault() {
        return Boolean.TRUE.equals(pinned);
    }
}
