package com.example.education_platform.course.entity;

import com.example.education_platform.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
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
@Table(name = "chapters")
public class Chapter extends BaseEntity implements Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String summary;

    /**
     * Batched rather than join-fetched: Hibernate cannot fetch two list collections in one query
     * (MultipleBagFetchException), so the course graph pulls chapters and this pulls every
     * chapter's lessons in one further query instead of one per chapter.
     */
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = 50)
    private List<Lesson> lessons = new ArrayList<>();

    public Chapter(String title, String summary) {
        this.title = title;
        this.summary = summary;
    }

    public Lesson addLesson(Lesson lesson, Integer position) {
        lesson.setChapter(this);
        Positions.insert(lessons, lesson, position);
        return lesson;
    }

    public void moveLesson(Lesson lesson, int position) {
        lessons.remove(lesson);
        Positions.insert(lessons, lesson, position);
    }

    public void removeLesson(Lesson lesson) {
        lessons.remove(lesson);
        Positions.renumber(lessons);
    }
}
