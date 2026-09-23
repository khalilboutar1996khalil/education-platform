package com.example.education_platform.quiz.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quiz_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_attempts_quiz_student_number",
                columnNames = {"quiz_id", "student_id", "attempt_number"}))
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    /**
     * Snapshotted when the attempt is handed in: the quiz may gain or lose questions afterwards,
     * and an old result must keep meaning what it meant at the time.
     */
    @Column(precision = 6, scale = 2)
    private BigDecimal maxScore;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<Answer> answers = new ArrayList<>();

    public QuizAttempt(Quiz quiz, User student, int attemptNumber, Instant startedAt) {
        this.quiz = quiz;
        this.student = student;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
    }

    /** Null duration means untimed, so such an attempt never expires on the clock. */
    public Instant expiresAt() {
        Integer minutes = quiz.getDurationMinutes();
        return minutes == null ? null : startedAt.plusSeconds(minutes * 60L);
    }

    public boolean hasRunOutOfTime(Instant now) {
        Instant expiry = expiresAt();
        if (expiry != null && !expiry.isAfter(now)) {
            return true;
        }
        Instant deadline = quiz.getDeadline();
        return deadline != null && !deadline.isAfter(now);
    }

    public Answer addAnswer(Answer answer) {
        answer.setAttempt(this);
        answers.add(answer);
        return answer;
    }
}
