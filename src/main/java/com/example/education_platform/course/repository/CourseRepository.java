package com.example.education_platform.course.repository;

import com.example.education_platform.course.entity.Course;
import com.example.education_platform.user.entity.Level;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** A null level means "every level", which is what an admin sees. */
    @Query("select c from Course c where :level is null or c.level = :level")
    Page<Course> findVisible(@Param("level") Level level, Pageable pageable);

    /**
     * The module detail screen. Chapters are join-fetched and their lessons arrive through
     * {@code @BatchSize}, so the whole tree costs two queries rather than one per chapter.
     */
    @EntityGraph(attributePaths = "chapters")
    Optional<Course> findDetailById(Long id);

    @Query("select c.course.id as courseId, count(c) as total from Chapter c group by c.course.id")
    List<CourseCount> countChaptersPerCourse();

    @Query("select l.chapter.course.id as courseId, count(l) as total from Lesson l group by l.chapter.course.id")
    List<CourseCount> countLessonsPerCourse();
}
