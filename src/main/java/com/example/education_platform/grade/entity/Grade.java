package com.example.education_platform.grade.entity;

import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One mark in one module, whatever produced it.
 *
 * <p>The source is a typed foreign key rather than a {@code source_id} integer, so the database
 * still guarantees that an automatic grade points at a row that exists. Exactly one of the two is
 * set for QUIZ and ASSIGNMENT, and neither for MANUAL — a CHECK constraint enforces that.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grades")
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeKind kind;

    /** What the student sees in the gradebook, e.g. "Partiel 1" or the quiz's title. */
    @Column(nullable = false)
    private String label;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal maxScore;

    /** Coefficient in the module average; 1.00 unless a teacher says otherwise. */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id")
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    /** Only for MANUAL entries; automatic ones are nobody's doing in particular. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id")
    private User recordedBy;

    public Grade(User student, Course course, GradeKind kind, String label,
                 BigDecimal score, BigDecimal maxScore) {
        this.student = student;
        this.course = course;
        this.kind = kind;
        this.label = label;
        this.score = score;
        this.maxScore = maxScore;
    }

    /**
     * The mark expressed out of 20, which is how every Algerian report card reads it. A grade with
     * a zero maximum contributes nothing rather than dividing by zero.
     */
    public BigDecimal outOfTwenty() {
        if (maxScore.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return score.multiply(BigDecimal.valueOf(20))
                .divide(maxScore, 2, RoundingMode.HALF_UP);
    }
}
