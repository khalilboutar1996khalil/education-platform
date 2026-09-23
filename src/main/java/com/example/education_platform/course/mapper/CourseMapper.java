package com.example.education_platform.course.mapper;

import com.example.education_platform.course.dto.response.ChapterResponse;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.dto.response.CourseSummaryResponse;
import com.example.education_platform.course.dto.response.LessonResponse;
import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import java.util.Set;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * The completed-lesson ids travel as a {@code @Context} so the flag reaches every nested lesson
 * without each level of the tree having to pass it along by hand.
 */
@Mapper
public interface CourseMapper {

    CourseSummaryResponse toSummary(Course course, long chapterCount, long lessonCount,
                                    Integer progressPercent, Integer classAveragePercent);

    CourseDetailResponse toDetail(Course course, @Context Set<Long> completedLessonIds);

    ChapterResponse toChapter(Chapter chapter, @Context Set<Long> completedLessonIds);

    @Mapping(target = "completed", expression = "java(completedLessonIds.contains(lesson.getId()))")
    LessonResponse toLesson(Lesson lesson, @Context Set<Long> completedLessonIds);
}
