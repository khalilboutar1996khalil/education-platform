package com.example.education_platform.dashboard.dto.response;

import java.time.Instant;

/** @param kind "QUIZ" or "ASSIGNMENT", so the client knows which screen to open */
public record DeadlineResponse(
        String kind,
        Long id,
        String title,
        String courseCode,
        Instant deadline) {
}
