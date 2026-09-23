package com.example.education_platform.course.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity implements Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonType type;

    /** Estimated time in minutes; null for items like a PDF or a TP. */
    private Integer durationMinutes;

    /** Link to the video, PDF or other resource backing this lesson. */
    private String contentUrl;

    @Column(length = 10_000)
    private String content;

    public Lesson(String title, LessonType type, Integer durationMinutes) {
        this.title = title;
        this.type = type;
        this.durationMinutes = durationMinutes;
    }
}
