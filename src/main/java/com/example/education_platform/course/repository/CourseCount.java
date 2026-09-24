package com.example.education_platform.course.repository;

/**
 * Projection for "count per course" aggregates. These exist so a list of modules costs one
 * extra query in total rather than one per module.
 */
public interface CourseCount {

    Long getCourseId();

    long getTotal();
}
