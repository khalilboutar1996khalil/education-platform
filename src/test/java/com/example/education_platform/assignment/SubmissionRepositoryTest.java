package com.example.education_platform.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.common.config.JpaAuditingConfig;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class SubmissionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-04-01T09:00:00Z");

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private SubmissionRepository submissions;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    private Assignment tp;
    private User amira;
    private User yacine;
    private User lina;

    @BeforeEach
    void seed() {
        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        amira = users.saveAndFlush(new User("Amira Benali", "amira@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));
        yacine = users.saveAndFlush(new User("Yacine Cherif", "yacine@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));
        lina = users.saveAndFlush(new User("Lina Boudiaf", "lina@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));

        tp = new Assignment(course, "TP n°1 — Calculatrice", AssignmentType.TP, WorkMode.PAIR);
        tp.setStatus(AssignmentStatus.OPEN);
        tp.setDeadline(NOW.plus(7, ChronoUnit.DAYS));
        tp = assignments.saveAndFlush(tp);
    }

    @Test
    void aPairSubmissionIsOneRowFoundFromEitherSide() {
        submissions.saveAndFlush(new Submission(tp, amira, yacine));

        assertThat(submissions.findMine(tp.getId(), amira.getId()))
                .describedAs("the owner finds it").isPresent();
        assertThat(submissions.findMine(tp.getId(), yacine.getId()))
                .describedAs("so does the partner").isPresent();
        assertThat(submissions.findMine(tp.getId(), lina.getId()))
                .describedAs("a stranger does not").isEmpty();
    }

    @Test
    void involvesRecognisesBothMembersAndNobodyElse() {
        Submission submission = submissions.saveAndFlush(new Submission(tp, amira, yacine));

        assertThat(submission.involves(amira.getId())).isTrue();
        assertThat(submission.involves(yacine.getId())).isTrue();
        assertThat(submission.involves(lina.getId())).isFalse();
    }

    @Test
    void aStudentCannotHandInTwiceForTheSameAssignment() {
        submissions.saveAndFlush(new Submission(tp, amira, null));

        assertThat(submissions.existsForUser(tp.getId(), amira.getId())).isTrue();
        assertThatThrownBy(() -> submissions.saveAndFlush(new Submission(tp, amira, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nobodyCanBeTheirOwnPartner() {
        assertThatThrownBy(() -> submissions.saveAndFlush(new Submission(tp, amira, amira)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aNegativeGradeIsRefusedByTheDatabase() {
        Submission submission = submissions.saveAndFlush(new Submission(tp, amira, null));
        submission.applyGrade(new BigDecimal("-1"), "Impossible", lina, NOW);

        assertThatThrownBy(() -> submissions.saveAndFlush(submission))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void submittingRecordsLatenessAtHandInTime() {
        Submission onTime = submissions.saveAndFlush(new Submission(tp, amira, null));
        onTime.submit(NOW, tp.isPastDeadline(NOW));

        Submission late = submissions.saveAndFlush(new Submission(tp, yacine, null));
        Instant afterDeadline = NOW.plus(8, ChronoUnit.DAYS);
        late.submit(afterDeadline, tp.isPastDeadline(afterDeadline));

        assertThat(onTime.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(late.getStatus()).isEqualTo(SubmissionStatus.LATE);
        assertThat(late.getSubmittedAt()).isEqualTo(afterDeadline);
    }

    @Test
    void theGradingQueueFiltersByStatus() {
        Submission handedIn = submissions.saveAndFlush(new Submission(tp, amira, null));
        handedIn.submit(NOW, false);
        submissions.saveAndFlush(new Submission(tp, yacine, null));
        submissions.flush();

        var waiting = submissions.findByAssignmentIdAndStatusIn(tp.getId(),
                EnumSet.of(SubmissionStatus.SUBMITTED, SubmissionStatus.LATE), PageRequest.of(0, 10));

        assertThat(waiting.getTotalElements())
                .describedAs("a draft is not waiting on the teacher").isEqualTo(1);
        assertThat(waiting.getContent().getFirst().getStudent().getEmail()).isEqualTo("amira@eduflow.dz");
    }

    @Test
    void gradingStampsTheMarkerAndClosesTheSubmission() {
        Submission submission = submissions.saveAndFlush(new Submission(tp, amira, null));
        submission.submit(NOW, false);

        submission.applyGrade(new BigDecimal("16.50"), "Bon travail", lina, NOW);
        submissions.flush();

        Submission reloaded = submissions.findDetailById(submission.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(reloaded.getGrade()).isEqualByComparingTo("16.50");
        assertThat(reloaded.getGradedBy().getEmail()).isEqualTo("lina@eduflow.dz");
        assertThat(reloaded.isEditable())
                .describedAs("a graded submission is no longer a draft").isFalse();
    }

    @Test
    void anAssignmentStopsAcceptingWorkOnceClosedOrPastItsDeadline() {
        assertThat(tp.acceptsSubmissionsAt(NOW)).isTrue();
        assertThat(tp.acceptsSubmissionsAt(NOW.plus(8, ChronoUnit.DAYS)))
                .describedAs("past the deadline without allowLate").isFalse();

        tp.setAllowLate(true);
        assertThat(tp.acceptsSubmissionsAt(NOW.plus(8, ChronoUnit.DAYS)))
                .describedAs("allowLate keeps it open, but the work is marked late").isTrue();

        tp.setStatus(AssignmentStatus.CLOSED);
        assertThat(tp.acceptsSubmissionsAt(NOW))
                .describedAs("closed beats allowLate").isFalse();
    }

    @Test
    void visibilityFiltersByLevelAndStatus() {
        var page = PageRequest.of(0, 10);

        assertThat(assignments.findVisible(Level.THIRD_AS, null,
                EnumSet.of(AssignmentStatus.OPEN), page).getTotalElements()).isEqualTo(1);
        assertThat(assignments.findVisible(Level.SECOND_AS, null,
                EnumSet.of(AssignmentStatus.OPEN), page).getTotalElements())
                .describedAs("another level sees nothing").isZero();
        assertThat(assignments.findVisible(null, null,
                EnumSet.of(AssignmentStatus.DRAFT), page).getTotalElements())
                .describedAs("this one is not a draft").isZero();
    }
}
