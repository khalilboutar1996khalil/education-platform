package com.example.education_platform.course.service.impl;

import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.dto.request.ChapterRequest;
import com.example.education_platform.course.dto.request.LessonRequest;
import com.example.education_platform.course.dto.response.ChapterResponse;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.mapper.CourseMapper;
import com.example.education_platform.course.repository.ChapterRepository;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonRepository;
import com.example.education_platform.course.service.CurriculumService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything goes through the parent aggregate rather than saving the child directly, because the
 * parent is what renumbers positions — writing a Chapter on its own would leave gaps or duplicates.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CurriculumServiceImpl implements CurriculumService {

    private final CourseRepository courses;
    private final ChapterRepository chapters;
    private final LessonRepository lessons;
    private final CourseMapper courseMapper;

    @Override
    public CourseDetailResponse addChapter(Long courseId, ChapterRequest request) {
        Course course = courses.findDetailById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        course.addChapter(new Chapter(request.title(), request.summary()), request.position());
        // Flush so the new row's generated id reaches the response
        courses.flush();
        return detailOf(course);
    }

    @Override
    public CourseDetailResponse updateChapter(Long chapterId, ChapterRequest request) {
        Chapter chapter = chapter(chapterId);
        chapter.setTitle(request.title());
        chapter.setSummary(request.summary());
        if (request.position() != null && request.position() != chapter.getPosition()) {
            chapter.getCourse().moveChapter(chapter, request.position());
        }
        return detailOf(chapter.getCourse());
    }

    @Override
    public CourseDetailResponse moveChapter(Long chapterId, int position) {
        Chapter chapter = chapter(chapterId);
        chapter.getCourse().moveChapter(chapter, position);
        return detailOf(chapter.getCourse());
    }

    @Override
    public void deleteChapter(Long chapterId) {
        Chapter chapter = chapter(chapterId);
        chapter.getCourse().removeChapter(chapter);
    }

    @Override
    public ChapterResponse addLesson(Long chapterId, LessonRequest request) {
        Chapter chapter = chapter(chapterId);
        Lesson lesson = new Lesson(request.title(), request.type(), request.durationMinutes());
        lesson.setContentUrl(request.contentUrl());
        lesson.setContent(request.content());
        chapter.addLesson(lesson, request.position());
        chapters.flush();
        return courseMapper.toChapter(chapter, Set.of());
    }

    @Override
    public ChapterResponse updateLesson(Long lessonId, LessonRequest request) {
        Lesson lesson = lesson(lessonId);
        lesson.setTitle(request.title());
        lesson.setType(request.type());
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setContentUrl(request.contentUrl());
        lesson.setContent(request.content());
        if (request.position() != null && request.position() != lesson.getPosition()) {
            lesson.getChapter().moveLesson(lesson, request.position());
        }
        return courseMapper.toChapter(lesson.getChapter(), Set.of());
    }

    @Override
    public ChapterResponse moveLesson(Long lessonId, int position) {
        Lesson lesson = lesson(lessonId);
        lesson.getChapter().moveLesson(lesson, position);
        return courseMapper.toChapter(lesson.getChapter(), Set.of());
    }

    @Override
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lesson(lessonId);
        lesson.getChapter().removeLesson(lesson);
    }

    private Chapter chapter(Long id) {
        return chapters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chapter", id));
    }

    private Lesson lesson(Long id) {
        return lessons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
    }

    /** Admin-facing, so no student context: nothing is flagged as completed. */
    private CourseDetailResponse detailOf(Course course) {
        return courseMapper.toDetail(course, Set.of());
    }
}
