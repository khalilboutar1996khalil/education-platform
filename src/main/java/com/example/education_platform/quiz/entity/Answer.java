package com.example.education_platform.quiz.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/** One student's response to one question. Never stores the question text — only the link. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_answers_attempt_question",
                columnNames = {"attempt_id", "question_id"}))
public class Answer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    /** Only for OPEN questions; the choice links carry everything else. */
    @Column(length = 5000)
    private String textAnswer;

    @Column(precision = 5, scale = 2)
    private BigDecimal awardedPoints;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "answer_choices",
            joinColumns = @JoinColumn(name = "answer_id"),
            inverseJoinColumns = @JoinColumn(name = "choice_id"))
    @BatchSize(size = 100)
    private Set<Choice> selectedChoices = new LinkedHashSet<>();

    public Answer(Question question) {
        this.question = question;
    }

    public Set<Long> selectedChoiceIds() {
        return selectedChoices.stream().map(Choice::getId).collect(Collectors.toSet());
    }

    /** Returns the points awarded, so the caller can total them without re-reading. */
    public BigDecimal grade() {
        BigDecimal awarded = question.scoreFor(selectedChoiceIds());
        this.awardedPoints = awarded;
        return awarded;
    }
}
