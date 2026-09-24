package com.example.education_platform.announcement.dto.response;

import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import java.time.Instant;

/**
 * @param publishedAt     null while it is a draft, which only an admin ever sees
 * @param recipientCount  how many students it went out to, frozen at publication
 */
public record AnnouncementResponse(
        Long id,
        String title,
        String body,
        UserResponse author,
        Long courseId,
        String courseCode,
        Level level,
        boolean pinned,
        Instant publishedAt,
        Integer recipientCount) {
}
