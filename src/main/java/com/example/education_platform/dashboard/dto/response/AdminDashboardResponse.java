package com.example.education_platform.dashboard.dto.response;

import java.util.List;

/**
 * @param submissionsAwaitingMarking what is actually waiting on the teacher right now
 * @param weeklySubmissions          the last seven days, oldest first
 * @param studentsByLevel            active students per level
 * @param coursesByLevel             modules per level
 */
public record AdminDashboardResponse(
        long activeStudents,
        long courses,
        long quizzesInProgress,
        long openAssignments,
        long submissionsAwaitingMarking,
        List<DayCountResponse> weeklySubmissions,
        List<DeadlineResponse> upcomingDeadlines,
        List<LevelCountResponse> studentsByLevel,
        List<LevelCountResponse> coursesByLevel) {
}
