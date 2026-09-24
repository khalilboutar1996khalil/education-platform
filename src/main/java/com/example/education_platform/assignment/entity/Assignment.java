package com.example.education_platform.assignment.entity;

import com.example.education_platform.common.BaseEntity;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.storage.entity.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A TP or a devoir. The two differ by {@code type} and {@code mode}, not by structure, which is
 * why one table covers both.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignments")
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 10_000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkMode mode = WorkMode.INDIVIDUAL;

    private Instant deadline;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPoints = new BigDecimal("20");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    /** When false, the deadline is a hard stop rather than a mark of lateness. */
    @Column(nullable = false)
    private boolean allowLate;

    /** The subject sheet handed out with the assignment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brief_file_id")
    private StoredFile brief;

    public Assignment(Course course, String title, AssignmentType type, WorkMode mode) {
        this.course = course;
        this.title = title;
        this.type = type;
        this.mode = mode;
    }

    public boolean isPastDeadline(Instant now) {
        return deadline != null && !deadline.isAfter(now);
    }

    /** Open for new work: a closed or draft assignment takes nothing, late or not. */
    public boolean acceptsSubmissionsAt(Instant now) {
        return status == AssignmentStatus.OPEN && (!isPastDeadline(now) || allowLate);
    }

    public boolean isPairWork() {
        return mode == WorkMode.PAIR;
    }
}
