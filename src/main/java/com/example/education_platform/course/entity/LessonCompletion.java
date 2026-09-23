package com.example.education_platform.course.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Records that a student finished a lesson; every progress percentage counts these rows.
 * Deliberately has no setters — a completion is created or deleted, never edited.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "lesson_completions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_completions_student_lesson",
                columnNames = {"student_id", "lesson_id"}))
public class LessonCompletion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lesson lesson;

    public LessonCompletion(User student, Lesson lesson) {
        this.student = student;
        this.lesson = lesson;
    }
}
