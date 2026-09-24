package com.example.education_platform.notification.job;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.notification.entity.NotificationType;
import com.example.education_platform.notification.service.NotificationService;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reminds students of work due within a day.
 *
 * <p>Only students who have not handed in are told, so the reminder is a nudge rather than noise.
 * The job runs hourly and the window is exactly one hour wide, which is what keeps it from
 * reminding the same people every hour — there is no "already reminded" flag to keep.
 */
@Component
@RequiredArgsConstructor
public class DeadlineReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineReminderJob.class);
    private static final Duration NOTICE = Duration.ofHours(24);
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int BATCH = 100;

    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final UserRepository users;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void run() {
        int reminded = remind(Instant.now());
        if (reminded > 0) {
            LOG.info("Deadline reminder job: notified {} students", reminded);
        }
    }

    public int remind(Instant now) {
        Instant from = now.plus(NOTICE);
        Instant to = from.plus(WINDOW);
        List<Assignment> dueSoon = assignments.findWithDeadlineBetween(
                AssignmentStatus.OPEN, from, to, PageRequest.of(0, BATCH));

        int reminded = 0;
        for (Assignment assignment : dueSoon) {
            List<User> pending = users
                    .findActiveStudents(Role.STUDENT, UserStatus.ACTIVE, assignment.getCourse().getLevel())
                    .stream()
                    // Nobody needs chasing for work they have already handed in
                    .filter(student -> !submissions.existsForUser(assignment.getId(), student.getId()))
                    .toList();

            notificationService.notifyAll(pending, NotificationType.DEADLINE,
                    "À rendre demain : " + assignment.getTitle(),
                    "Le TP « " + assignment.getTitle() + " » est à rendre dans 24 heures.",
                    "/assignments/" + assignment.getId());
            reminded += pending.size();
        }
        return reminded;
    }
}
