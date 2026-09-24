package com.example.education_platform.announcement.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A notice to the whole section or to one module. Scope follows the same rule as the library:
 * two nullable columns where null means "everyone".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "announcements")
public class Announcement extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    /** Null means the whole section rather than one module. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /** Null means every level. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Level level;

    @Column(nullable = false)
    private boolean pinned;

    /** Null while it is still a draft; students never see those. */
    private Instant publishedAt;

    /**
     * How many students it went out to, frozen at publication. The audience changes as accounts
     * come and go, and "who read this" is a question about the day it was sent.
     */
    private Integer recipientCount;

    public Announcement(String title, String body, User author, Course course, Level level) {
        this.title = title;
        this.body = body;
        this.author = author;
        this.course = course;
        this.level = level;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    /** The level this notice actually reaches: a module-scoped one inherits its module's level. */
    public Level targetLevel() {
        if (course != null) {
            return course.getLevel();
        }
        return level;
    }

    public boolean isVisibleTo(Level studentLevel) {
        if (studentLevel == null) {
            return true;
        }
        boolean courseAllows = course == null || course.getLevel() == studentLevel;
        boolean levelAllows = level == null || level == studentLevel;
        return isPublished() && courseAllows && levelAllows;
    }

    public void publish(Instant now, int recipients) {
        this.publishedAt = now;
        this.recipientCount = recipients;
    }
}
