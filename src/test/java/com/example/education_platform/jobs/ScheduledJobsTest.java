package com.example.education_platform.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.job.AssignmentDeadlineJob;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.notification.job.DeadlineReminderJob;
import com.example.education_platform.notification.repository.NotificationRepository;
import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.QuestionType;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.job.QuizDeadlineJob;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.quiz.repository.QuizRepository;
import com.example.education_platform.storage.job.OrphanedFileSweeper;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The jobs are driven directly rather than waited for. What matters is what each one decides;
 * the cron expression is Spring's business, not the test's.
 */
@SpringBootTest
@Transactional
class ScheduledJobsTest {

    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    @Autowired
    private QuizDeadlineJob quizJob;

    @Autowired
    private AssignmentDeadlineJob assignmentJob;

    @Autowired
    private DeadlineReminderJob reminderJob;

    @Autowired
    private OrphanedFileSweeper sweeper;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private QuizRepository quizzes;

    @Autowired
    private QuizAttemptRepository attempts;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserRepository users;

    private Course course;
    private User amira;

    @BeforeEach
    void seed() {
        course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        amira = users.saveAndFlush(new User("Amira Benali", "amira@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));
    }

    // ---------- quizzes ----------

    @Test
    void anOverdueQuizIsClosed() {
        Long onTime = quizWithDeadline(NOW.plus(1, ChronoUnit.DAYS));
        Long overdue = quizWithDeadline(NOW.minus(1, ChronoUnit.HOURS));

        assertThat(quizJob.closeOverdueQuizzes(NOW)).isEqualTo(1);
        quizzes.flush();

        assertThat(quizzes.findById(overdue).orElseThrow().getStatus()).isEqualTo(QuizStatus.CLOSED);
        assertThat(quizzes.findById(onTime).orElseThrow().getStatus())
                .describedAs("still within its deadline").isEqualTo(QuizStatus.IN_PROGRESS);
    }

    @Test
    void anAbandonedAttemptIsExpiredInsteadOfBlockingTheNextOne() {
        Quiz quiz = quizzes.findById(quizWithDeadline(NOW.plus(1, ChronoUnit.DAYS))).orElseThrow();
        quiz.setDurationMinutes(30);
        QuizAttempt stale = attempts.saveAndFlush(
                new QuizAttempt(quiz, amira, 1, NOW.minus(2, ChronoUnit.HOURS)));

        assertThat(quizJob.expireStaleAttempts(NOW)).isEqualTo(1);
        attempts.flush();

        assertThat(attempts.findById(stale.getId()).orElseThrow().getStatus())
                .describedAs("the clock ran out two hours ago").isEqualTo(AttemptStatus.EXPIRED);
    }

    @Test
    void anAttemptStillWithinItsClockIsLeftAlone() {
        Quiz quiz = quizzes.findById(quizWithDeadline(NOW.plus(1, ChronoUnit.DAYS))).orElseThrow();
        quiz.setDurationMinutes(60);
        QuizAttempt running = attempts.saveAndFlush(
                new QuizAttempt(quiz, amira, 1, NOW.minus(10, ChronoUnit.MINUTES)));

        assertThat(quizJob.expireStaleAttempts(NOW)).isZero();
        assertThat(attempts.findById(running.getId()).orElseThrow().getStatus())
                .isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    // ---------- assignments ----------

    @Test
    void anOverdueAssignmentIsClosedUnlessLateWorkIsAllowed() {
        Long strict = assignmentWithDeadline(NOW.minus(1, ChronoUnit.HOURS), false);
        Long lenient = assignmentWithDeadline(NOW.minus(1, ChronoUnit.HOURS), true);

        assertThat(assignmentJob.closeOverdue(NOW)).isEqualTo(1);
        assignments.flush();

        assertThat(assignments.findById(strict).orElseThrow().getStatus())
                .isEqualTo(AssignmentStatus.CLOSED);
        assertThat(assignments.findById(lenient).orElseThrow().getStatus())
                .describedAs("allowLate means the deadline marks lateness, not closure")
                .isEqualTo(AssignmentStatus.OPEN);
    }

    // ---------- reminders ----------

    @Test
    void studentsWhoHaveNotHandedInAreRemindedADayBefore() {
        assignmentWithDeadline(NOW.plus(24, ChronoUnit.HOURS).plusSeconds(60), false);

        assertThat(reminderJob.remind(NOW)).isEqualTo(1);
        assertThat(notifications.countByRecipientIdAndReadAtIsNull(amira.getId())).isEqualTo(1);
    }

    @Test
    void workDueNextWeekPromptsNothingYet() {
        assignmentWithDeadline(NOW.plus(7, ChronoUnit.DAYS), false);

        assertThat(reminderJob.remind(NOW))
                .describedAs("the window is one hour wide, a day out").isZero();
        assertThat(notifications.countByRecipientIdAndReadAtIsNull(amira.getId())).isZero();
    }

    // ---------- files ----------

    @Test
    void theSweeperLeavesAFileNothingHasHadTimeToClaim() {
        // Nothing uploaded in this test, so the only assertion worth making is that it is safe
        assertThat(sweeper.sweep(NOW)).isZero();
    }

    // ---------- helpers ----------

    private Long quizWithDeadline(Instant deadline) {
        Quiz quiz = new Quiz(course, "Contrôle", null);
        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quiz.setDeadline(deadline);
        Question question = new Question("2 + 2 ?", QuestionType.SINGLE_CHOICE, BigDecimal.ONE);
        question.addChoice(new Choice("4", true), null);
        question.addChoice(new Choice("3", false), null);
        quiz.addQuestion(question, null);
        return quizzes.saveAndFlush(quiz).getId();
    }

    private Long assignmentWithDeadline(Instant deadline, boolean allowLate) {
        Assignment assignment = new Assignment(course, "TP", AssignmentType.TP, WorkMode.INDIVIDUAL);
        assignment.setStatus(AssignmentStatus.OPEN);
        assignment.setDeadline(deadline);
        assignment.setAllowLate(allowLate);
        return assignments.saveAndFlush(assignment).getId();
    }
}
