package com.example.education_platform.notification.listener;

import com.example.education_platform.announcement.entity.Announcement;
import com.example.education_platform.announcement.event.AnnouncementPublished;
import com.example.education_platform.announcement.repository.AnnouncementRepository;
import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.event.SubmissionGraded;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.notification.entity.ActivityType;
import com.example.education_platform.notification.entity.NotificationType;
import com.example.education_platform.notification.service.NotificationService;
import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.event.QuizAttemptGraded;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into notifications and feed entries.
 *
 * <p>Same one-way rule as the gradebook: this package knows about quizzes, assignments and
 * announcements, and none of them knows about notifications. Two listeners on the same event is
 * exactly what events are for — the gradebook records a mark, this tells the student about it.
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final SubmissionRepository submissions;
    private final QuizAttemptRepository attempts;
    private final AnnouncementRepository announcements;
    private final UserRepository users;

    @EventListener
    public void on(SubmissionGraded event) {
        Submission submission = submissions.findDetailById(event.submissionId()).orElse(null);
        if (submission == null) {
            return;
        }
        String title = submission.getAssignment().getTitle() + " a été corrigé";
        String body = "Note : " + submission.getGrade() + "/" + submission.getAssignment().getMaxPoints();
        String link = "/assignments/" + submission.getAssignment().getId();

        // A pair shares the work, so both hear about the mark
        List<User> recipients = new ArrayList<>();
        recipients.add(submission.getStudent());
        if (submission.getPartner() != null) {
            recipients.add(submission.getPartner());
        }
        notificationService.notifyAll(recipients, NotificationType.GRADED, title, body, link);

        notificationService.record(submission.getGradedBy(), ActivityType.SUBMISSION_GRADED,
                submission.getAssignment().getTitle() + " corrigé pour "
                        + submission.getStudent().getFullName(),
                "submission", submission.getId(), submission.getAssignment().getCourse());
    }

    @EventListener
    public void on(QuizAttemptGraded event) {
        QuizAttempt attempt = attempts.findById(event.attemptId()).orElse(null);
        if (attempt == null || attempt.getStatus() != AttemptStatus.GRADED) {
            // Still waiting on a human to mark an open question: nothing to announce yet
            return;
        }
        notificationService.notify(attempt.getStudent(), NotificationType.QUIZ_SCORED,
                attempt.getQuiz().getTitle() + " : résultat disponible",
                "Score : " + attempt.getScore() + "/" + attempt.getMaxScore(),
                "/quizzes/" + attempt.getQuiz().getId());

        notificationService.record(attempt.getStudent(), ActivityType.QUIZ_FINISHED,
                attempt.getStudent().getFullName() + " a terminé " + attempt.getQuiz().getTitle(),
                "quiz_attempt", attempt.getId(), attempt.getQuiz().getCourse());
    }

    @EventListener
    public void on(AnnouncementPublished event) {
        Announcement announcement = announcements.findDetailById(event.announcementId()).orElse(null);
        if (announcement == null) {
            return;
        }
        List<User> audience = users.findActiveStudents(Role.STUDENT, UserStatus.ACTIVE,
                announcement.targetLevel());
        notificationService.notifyAll(audience, NotificationType.ANNOUNCEMENT,
                announcement.getTitle(), announcement.getBody(),
                "/announcements/" + announcement.getId());

        notificationService.record(announcement.getAuthor(), ActivityType.ANNOUNCEMENT_PUBLISHED,
                "Annonce publiée : " + announcement.getTitle(),
                "announcement", announcement.getId(), announcement.getCourse());
    }
}
