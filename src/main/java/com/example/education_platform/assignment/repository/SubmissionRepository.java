package com.example.education_platform.assignment.repository;

import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @EntityGraph(attributePaths = {"assignment", "student", "partner", "files"})
    Optional<Submission> findDetailById(Long id);

    /** The owner's row. A partner's own view comes from {@link #findMine}, not from here. */
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    /** Either side of a pair counts, which is why this cannot be a derived query on one column. */
    @Query("""
            select s from Submission s
            where s.assignment.id = :assignmentId
              and (s.student.id = :userId or s.partner.id = :userId)
            """)
    Optional<Submission> findMine(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    /** The grading queue for one assignment. */
    @EntityGraph(attributePaths = {"student", "partner"})
    Page<Submission> findByAssignmentIdAndStatusIn(Long assignmentId,
                                                   Collection<SubmissionStatus> statuses,
                                                   Pageable pageable);

    @EntityGraph(attributePaths = {"assignment", "student", "partner"})
    @Query("""
            select s from Submission s
            where s.student.id = :userId or s.partner.id = :userId
            """)
    Page<Submission> findAllMine(@Param("userId") Long userId, Pageable pageable);

    /** Guards the pair rule: someone already handed in for this assignment. */
    @Query("""
            select count(s) > 0 from Submission s
            where s.assignment.id = :assignmentId
              and (s.student.id = :userId or s.partner.id = :userId)
            """)
    boolean existsForUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);
}
