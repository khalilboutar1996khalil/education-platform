package com.example.education_platform.notification.dto.response;

import com.example.education_platform.notification.entity.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        String link,
        boolean read,
        Instant createdAt) {
}
