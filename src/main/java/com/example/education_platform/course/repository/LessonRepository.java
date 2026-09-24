package com.example.education_platform.course.repository;

import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.user.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Denominator of a student's overall progress: every lesson at their level. */
    @Query("select count(l) from Lesson l where l.chapter.course.level = :level")
    long countAtLevel(@Param("level") Level level);
}
