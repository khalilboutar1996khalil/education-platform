package com.example.education_platform.grade.repository;

import com.example.education_platform.grade.entity.Grade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    /** Re-marking must update the existing row, which is what the partial unique index enforces. */
    Optional<Grade> findByQuizAttemptId(Long quizAttemptId);

    Optional<Grade> findBySubmissionId(Long submissionId);

    @EntityGraph(attributePaths = "course")
    List<Grade> findByStudentIdOrderByCourseIdAscCreatedAtAsc(Long studentId);

    @EntityGraph(attributePaths = {"course", "student"})
    List<Grade> findByCourseIdAndStudentIdOrderByCreatedAtAsc(Long courseId, Long studentId);

    @EntityGraph(attributePaths = "student")
    List<Grade> findByCourseIdOrderByStudentIdAsc(Long courseId);

    /** Everyone who has at least one mark in this module, which is who the gradebook lists. */
    @Query("select distinct g.student.id from Grade g where g.course.id = :courseId")
    List<Long> findStudentIdsWithGrades(@Param("courseId") Long courseId);
}
