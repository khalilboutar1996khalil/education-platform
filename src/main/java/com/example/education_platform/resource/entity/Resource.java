package com.example.education_platform.resource.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.storage.entity.StoredFile;
import com.example.education_platform.user.entity.Level;
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

/**
 * One item in the library. Scope is expressed by two nullable columns where null means "everyone":
 * no course and no level makes it visible to the whole section.
 *
 * <p>A resource is either an upload or a link, never both — a CHECK constraint enforces that.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resources")
public class Resource extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    /** Null means every module. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /** Null means every level. */
    @Column(length = 20)
    private Level level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    /** Set only when {@code type} is LINK. */
    @Column(length = 1000)
    private String externalUrl;

    /** Incremented on each download rather than recomputed, since nothing else records the event. */
    @Column(nullable = false)
    private long downloadCount;

    public Resource(String title, String description, ResourceType type, Course course, Level level) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.course = course;
        this.level = level;
    }

    public boolean isLink() {
        return type == ResourceType.LINK;
    }

    /**
     * A student sees it when neither scope excludes them. An admin passes null and sees everything.
     */
    public boolean isVisibleTo(Level studentLevel) {
        if (studentLevel == null) {
            return true;
        }
        boolean courseAllows = course == null || course.getLevel().equals(studentLevel);
        boolean levelAllows = level == null || level.equals(studentLevel);
        return courseAllows && levelAllows;
    }
}
