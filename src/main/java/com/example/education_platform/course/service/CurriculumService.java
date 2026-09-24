package com.example.education_platform.course.service;

import com.example.education_platform.course.dto.request.ChapterRequest;
import com.example.education_platform.course.dto.request.LessonRequest;
import com.example.education_platform.course.dto.response.ChapterResponse;
import com.example.education_platform.course.dto.response.CourseDetailResponse;

/**
 * Chapter and lesson editing. Every change returns the reordered parent, because a move or a
 * delete renumbers the siblings and the client would otherwise be showing stale positions.
 */
public interface CurriculumService {

    CourseDetailResponse addChapter(Long courseId, ChapterRequest request);

    CourseDetailResponse updateChapter(Long chapterId, ChapterRequest request);

    CourseDetailResponse moveChapter(Long chapterId, int position);

    void deleteChapter(Long chapterId);

    ChapterResponse addLesson(Long chapterId, LessonRequest request);

    ChapterResponse updateLesson(Long lessonId, LessonRequest request);

    ChapterResponse moveLesson(Long lessonId, int position);

    void deleteLesson(Long lessonId);
}
