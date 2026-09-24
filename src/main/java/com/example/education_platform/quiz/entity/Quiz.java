package com.example.education_platform.quiz.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.common.Positions;
import com.example.education_platform.course.entity.Course;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
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
@Table(name = "quizzes")
public class Quiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status = QuizStatus.DRAFT;

    /** Null means untimed; otherwise the attempt expires this many minutes after it started. */
    private Integer durationMinutes;

    private Instant opensAt;

    private Instant deadline;

    @Column(nullable = false)
    private int maxAttempts = 1;

    @Column(nullable = false)
    private boolean shuffleQuestions;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = 50)
    private List<Question> questions = new ArrayList<>();

    public Quiz(Course course, String title, String description) {
        this.course = course;
        this.title = title;
        this.description = description;
    }

    public Question addQuestion(Question question, Integer position) {
        question.setQuiz(this);
        Positions.insert(questions, question, position);
        return question;
    }

    public void moveQuestion(Question question, int position) {
        questions.remove(question);
        Positions.insert(questions, question, position);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        Positions.renumber(questions);
    }

    /** Derived rather than stored, so editing a question can never leave a stale total behind. */
    public BigDecimal totalPoints() {
        return questions.stream().map(Question::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isOpenAt(Instant now) {
        return status == QuizStatus.IN_PROGRESS
                && (opensAt == null || !opensAt.isAfter(now))
                && (deadline == null || deadline.isAfter(now));
    }

    /** Every question can be scored without a human. */
    public boolean isFullyAutoGradable() {
        return questions.stream().map(Question::getType).allMatch(QuestionType::isAutoGradable);
    }
}
