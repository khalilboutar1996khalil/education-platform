package com.example.education_platform.course.repository;

import com.example.education_platform.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
