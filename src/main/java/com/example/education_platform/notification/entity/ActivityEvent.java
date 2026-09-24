package com.example.education_platform.notification.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.course.entity.Course;
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
 * The dashboard's "recent activity" feed.
 *
 * <p>The one place a loose subject reference is right: this is append-only, never joined back
 * from, and has to survive the deletion of whatever it describes. Everywhere else in the model a
 * typed foreign key earns its keep; here it would only stop the log outliving its subject.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activity_events")
public class ActivityEvent extends BaseEntity {

    /** Null for something the system did rather than a person. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityType type;

    /** A short human line, composed when the event happens so the feed needs no joins to read. */
    @Column(nullable = false, length = 500)
    private String summary;

    @Column(length = 40)
    private String subjectType;

    /** Deliberately not a foreign key — see the class comment. */
    private Long subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private Instant occurredAt;

    public ActivityEvent(User actor, ActivityType type, String summary,
                         String subjectType, Long subjectId, Course course, Instant occurredAt) {
        this.actor = actor;
        this.type = type;
        this.summary = summary;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.course = course;
        this.occurredAt = occurredAt;
    }
}
