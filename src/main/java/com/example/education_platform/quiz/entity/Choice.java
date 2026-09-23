package com.example.education_platform.quiz.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.common.Positioned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** {@code correct} must never reach a student: the response DTO projects id and text only. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "choices")
public class Choice extends BaseEntity implements Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    public Choice(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }
}
