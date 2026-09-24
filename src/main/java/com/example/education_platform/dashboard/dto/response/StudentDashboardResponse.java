package com.example.education_platform.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param overallAverage null when nothing has been marked yet, rather than zero
 * @param progressPercent lessons finished across every module of the student's level
 */
public record StudentDashboardResponse(
        long courses,
        long lessonsCompleted,
        long totalLessons,
        int progressPercent,
        BigDecimal overallAverage,
        long assignmentsToHandIn,
        long quizzesOpen,
        long unreadNotifications,
        List<DeadlineResponse> upcomingDeadlines) {
}
