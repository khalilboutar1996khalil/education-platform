package com.example.education_platform.assignment.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * One student's work, or one pair's. A pair hands in a single row with two members rather than two
 * rows, so one upload and one grade serve both.
 *
 * <p>The correction lives here too: a grade has no life of its own once the work it marks is gone.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submissions_assignment_student",
                columnNames = {"assignment_id", "student_id"}))
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    /** The owner: the one who created the submission and the one the uniqueness rule is about. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    /** Set only in PAIR mode; sees and shares everything the owner does. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private User partner;

    @Column(length = 2000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    private Instant submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal grade;

    @Column(length = 5000)
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_id")
    private User gradedBy;

    private Instant gradedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "submission_files",
            joinColumns = @JoinColumn(name = "submission_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id"))
    @BatchSize(size = 50)
    private Set<StoredFile> files = new LinkedHashSet<>();

    public Submission(Assignment assignment, User student, User partner) {
        this.assignment = assignment;
        this.student = student;
        this.partner = partner;
    }

    /** Both members of a pair count as owners; everyone else is a stranger to this row. */
    public boolean involves(Long userId) {
        return student.getId().equals(userId) || (partner != null && partner.getId().equals(userId));
    }

    public boolean isEditable() {
        return status == SubmissionStatus.DRAFT;
    }

    /** Lateness is recorded at hand-in, not computed later, so moving the deadline cannot rewrite it. */
    public void submit(Instant now, boolean late) {
        this.submittedAt = now;
        this.status = late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED;
    }

    public void applyGrade(BigDecimal value, String feedback, User grader, Instant now) {
        this.grade = value;
        this.feedback = feedback;
        this.gradedBy = grader;
        this.gradedAt = now;
        this.status = SubmissionStatus.GRADED;
    }
}
