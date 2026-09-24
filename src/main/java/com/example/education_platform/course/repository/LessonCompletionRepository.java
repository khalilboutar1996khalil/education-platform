package com.example.education_platform.course.repository;

import com.example.education_platform.course.entity.LessonCompletion;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    boolean existsByStudentIdAndLessonId(Long studentId, Long lessonId);

    long countByStudentId(Long studentId);

    long deleteByStudentIdAndLessonId(Long studentId, Long lessonId);

    @Query("""
            select lc.lesson.id from LessonCompletion lc
            where lc.student.id = :studentId and lc.lesson.chapter.course.id = :courseId""")
    Set<Long> findCompletedLessonIds(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("""
            select lc.lesson.chapter.course.id as courseId, count(lc) as total from LessonCompletion lc
            where lc.student.id = :studentId group by lc.lesson.chapter.course.id""")
    List<CourseCount> countPerCourseForStudent(@Param("studentId") Long studentId);

    /** Drives the class average: how many completions all students have in each module. */
    @Query("""
            select lc.lesson.chapter.course.id as courseId, count(lc) as total from LessonCompletion lc
            group by lc.lesson.chapter.course.id""")
    List<CourseCount> countPerCourse();
}
