package com.example.education_platform.dashboard.service.impl;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.common.config.CacheConfig;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonCompletionRepository;
import com.example.education_platform.course.repository.LessonRepository;
import com.example.education_platform.dashboard.dto.response.AdminDashboardResponse;
import com.example.education_platform.dashboard.dto.response.DayCountResponse;
import com.example.education_platform.dashboard.dto.response.DeadlineResponse;
import com.example.education_platform.dashboard.dto.response.StudentDashboardResponse;
import com.example.education_platform.grade.service.GradebookService;
import com.example.education_platform.notification.repository.NotificationRepository;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.repository.QuizRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cached half of the dashboard, kept in its own bean on purpose: a {@code @Cacheable} method
 * called from inside the same class goes straight through and the cache never sees it.
 *
 * <p>Every key carries the id of whoever asked. A dashboard keyed on anything less would hand one
 * student another student's figures.
 */
@Component
@RequiredArgsConstructor
public class DashboardQueries {

    private static final int DEADLINE_LIMIT = 5;
    private static final int CHART_DAYS = 7;
    private static final EnumSet<SubmissionStatus> AWAITING_MARKING =
            EnumSet.of(SubmissionStatus.SUBMITTED, SubmissionStatus.LATE);

    private final UserRepository users;
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final LessonCompletionRepository completions;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final QuizRepository quizzes;
    private final NotificationRepository notifications;
    private final GradebookService gradebook;

    @Cacheable(cacheNames = CacheConfig.DASHBOARD_CACHE, key = "'admin:' + #adminId")
    @Transactional(readOnly = true)
    public AdminDashboardResponse forAdmin(Long adminId) {
        Instant now = Instant.now();
        return new AdminDashboardResponse(
                users.countStudents(Role.STUDENT, UserStatus.ACTIVE, null),
                courses.count(),
                quizzes.countByStatus(QuizStatus.IN_PROGRESS),
                assignments.countByStatus(AssignmentStatus.OPEN),
                submissions.countByStatusIn(AWAITING_MARKING),
                weeklySubmissions(now),
                upcomingDeadlines(null, now));
    }

    @Cacheable(cacheNames = CacheConfig.DASHBOARD_CACHE, key = "'student:' + #studentId")
    @Transactional(readOnly = true)
    public StudentDashboardResponse forStudent(Long studentId, Level level) {
        Instant now = Instant.now();
        long finished = completions.countByStudentId(studentId);
        long total = lessons.countAtLevel(level);

        return new StudentDashboardResponse(
                courses.findVisible(level, PageRequest.of(0, 1)).getTotalElements(),
                finished,
                total,
                percent(finished, total),
                gradebook.overallAverage(studentId),
                submissions.countOutstandingFor(studentId, level),
                quizzes.findVisible(level, null, EnumSet.of(QuizStatus.IN_PROGRESS),
                        PageRequest.of(0, 1)).getTotalElements(),
                notifications.countByRecipientIdAndReadAtIsNull(studentId),
                upcomingDeadlines(level, now));
    }

    // ---------- pieces ----------

    /** Quizzes and assignments share one list, soonest first, because a student has one calendar. */
    private List<DeadlineResponse> upcomingDeadlines(Level level, Instant now) {
        var page = PageRequest.of(0, DEADLINE_LIMIT);
        List<DeadlineResponse> combined = new ArrayList<>();

        for (Assignment assignment : assignments.findUpcoming(AssignmentStatus.OPEN, level, now, page)) {
            combined.add(new DeadlineResponse("ASSIGNMENT", assignment.getId(), assignment.getTitle(),
                    assignment.getCourse().getCode(), assignment.getDeadline()));
        }
        for (Quiz quiz : quizzes.findUpcoming(QuizStatus.IN_PROGRESS, level, now, page)) {
            combined.add(new DeadlineResponse("QUIZ", quiz.getId(), quiz.getTitle(),
                    quiz.getCourse().getCode(), quiz.getDeadline()));
        }
        return combined.stream()
                .sorted(Comparator.comparing(DeadlineResponse::deadline))
                .limit(DEADLINE_LIMIT)
                .toList();
    }

    /** Seven days ending today. A quiet day is a zero, not a gap the chart has to guess at. */
    private List<DayCountResponse> weeklySubmissions(Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate from = today.minusDays(CHART_DAYS - 1L);

        Map<LocalDate, Long> counted = submissions
                .findSubmittedAtSince(from.atStartOfDay(ZoneOffset.UTC).toInstant())
                .stream()
                .collect(Collectors.groupingBy(
                        instant -> instant.atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()));

        return LongStream.range(0, CHART_DAYS)
                .mapToObj(offset -> from.plusDays(offset))
                .map(day -> new DayCountResponse(day, counted.getOrDefault(day, 0L)))
                .toList();
    }

    private static int percent(long done, long total) {
        return total <= 0 ? 0 : Math.toIntExact(Math.round(100.0 * done / total));
    }
}
