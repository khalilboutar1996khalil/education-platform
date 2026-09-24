package com.example.education_platform.assignment.repository;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.user.entity.Level;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @EntityGraph(attributePaths = {"course", "brief"})
    Optional<Assignment> findDetailById(Long id);

    /**
     * A student passes their own level and the statuses they may see; an admin passes a null level
     * and every status. The course is fetched because each row renders its module code.
     */
    @EntityGraph(attributePaths = "course")
    @Query("""
            select a from Assignment a
            where (:level is null or a.course.level = :level)
              and (:courseId is null or a.course.id = :courseId)
              and a.status in :statuses
            """)
    Page<Assignment> findVisible(@Param("level") Level level,
                                 @Param("courseId") Long courseId,
                                 @Param("statuses") Collection<AssignmentStatus> statuses,
                                 Pageable pageable);

    /** What is coming up. Course is mandatory here, so the implicit join is correctly an inner one. */
    @EntityGraph(attributePaths = "course")
    @Query("""
            select a from Assignment a
            where a.status = :status and a.deadline is not null and a.deadline > :now
              and (:level is null or a.course.level = :level)
            order by a.deadline
            """)
    List<Assignment> findUpcoming(@Param("status") AssignmentStatus status,
                                  @Param("level") Level level,
                                  @Param("now") Instant now,
                                  Pageable pageable);

    long countByStatus(AssignmentStatus status);

    /** Work falling due inside a window; the reminder job walks these. */
    @EntityGraph(attributePaths = "course")
    @Query("""
            select a from Assignment a
            where a.status = :status and a.deadline between :from and :to
            order by a.deadline
            """)
    List<Assignment> findWithDeadlineBetween(@Param("status") AssignmentStatus status,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to,
                                             Pageable pageable);

    /** Still open although the deadline has gone; the job closes the ones that forbid late work. */
    @Query("select a from Assignment a where a.status = :status and a.deadline is not null and a.deadline <= :now")
    List<Assignment> findOverdue(@Param("status") AssignmentStatus status, @Param("now") Instant now);
}
