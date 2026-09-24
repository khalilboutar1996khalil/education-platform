package com.example.education_platform.notification.dto.response;

import com.example.education_platform.notification.entity.ActivityType;
import java.time.Instant;

/** @param actorName null when the system did it rather than a person */
public record ActivityResponse(
        Long id,
        ActivityType type,
        String summary,
        String actorName,
        String courseCode,
        Instant occurredAt) {
}
