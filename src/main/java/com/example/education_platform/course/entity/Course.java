package com.example.education_platform.course.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.user.entity.Level;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A teaching module (e.g. INF201 · Algorithmique et programmation) for one level. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    /** Accent colour used by the UI, e.g. #16A34A. */
    @Column(length = 7)
    private String color;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Chapter> chapters = new ArrayList<>();

    public Course(String code, String title, String description, Level level, String color) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.level = level;
        this.color = color;
    }

    public Chapter addChapter(Chapter chapter, Integer position) {
        chapter.setCourse(this);
        Positions.insert(chapters, chapter, position);
        return chapter;
    }

    public void moveChapter(Chapter chapter, int position) {
        chapters.remove(chapter);
        Positions.insert(chapters, chapter, position);
    }

    public void removeChapter(Chapter chapter) {
        chapters.remove(chapter);
        Positions.renumber(chapters);
    }

    public int lessonCount() {
        return chapters.stream().mapToInt(chapter -> chapter.getLessons().size()).sum();
    }
}
