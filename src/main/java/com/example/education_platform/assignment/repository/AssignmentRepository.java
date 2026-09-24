package com.example.education_platform.assignment.repository;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.user.entity.Level;
import java.util.Collection;
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
}
