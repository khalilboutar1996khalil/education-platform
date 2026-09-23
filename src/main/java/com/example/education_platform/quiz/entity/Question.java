package com.example.education_platform.quiz.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.common.Positioned;
import com.example.education_platform.common.Positions;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question extends BaseEntity implements Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal points = BigDecimal.ONE;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = 50)
    private List<Choice> choices = new ArrayList<>();

    public Question(String text, QuestionType type, BigDecimal points) {
        this.text = text;
        this.type = type;
        this.points = points;
    }

    public Choice addChoice(Choice choice, Integer position) {
        choice.setQuestion(this);
        Positions.insert(choices, choice, position);
        return choice;
    }

    public void removeChoice(Choice choice) {
        choices.remove(choice);
        Positions.renumber(choices);
    }

    public Set<Long> correctChoiceIds() {
        return choices.stream().filter(Choice::isCorrect).map(Choice::getId).collect(Collectors.toSet());
    }

    /**
     * All-or-nothing: a multiple-choice answer scores only when it matches the key exactly, so a
     * student cannot tick every box and collect the points.
     */
    public BigDecimal scoreFor(Set<Long> selectedChoiceIds) {
        if (!type.isAutoGradable()) {
            return BigDecimal.ZERO;
        }
        return correctChoiceIds().equals(selectedChoiceIds) ? points : BigDecimal.ZERO;
    }
}
