package com.example.education_platform.course.service.impl;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.dto.request.CourseRequest;
import com.example.education_platform.course.dto.response.CourseDetailResponse;
import com.example.education_platform.course.dto.response.CourseSummaryResponse;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.mapper.CourseMapper;
import com.example.education_platform.course.repository.CourseCount;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonCompletionRepository;
import com.example.education_platform.course.service.CourseService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courses;
    private final LessonCompletionRepository completions;
    private final UserRepository users;
    private final CourseMapper courseMapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> list(Level level, Pageable pageable) {
        User me = currentUser.get();
        Level effectiveLevel = me.isAdmin() ? level : me.getLevel();
        Page<Course> page = courses.findVisible(effectiveLevel, pageable);

        // Three aggregate queries for the whole page, rather than three per module
        Map<Long, Long> chapterCounts = totals(courses.countChaptersPerCourse());
        Map<Long, Long> lessonCounts = totals(courses.countLessonsPerCourse());
        Map<Long, Long> completionCounts = me.isAdmin()
                ? totals(completions.countPerCourse())
                : totals(completions.countPerCourseForStudent(me.getId()));

        List<CourseSummaryResponse> content = page.getContent().stream()
                .map(course -> toSummary(course, me, chapterCounts, lessonCounts, completionCounts))
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getDetail(Long id) {
        User me = currentUser.get();
        Course course = courses.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        requireVisible(me, course);

        Set<Long> completedLessonIds = me.isAdmin()
                ? Set.of()
                : completions.findCompletedLessonIds(me.getId(), id);
        return courseMapper.toDetail(course, completedLessonIds);
    }

    @Override
    @Transactional
    public CourseDetailResponse create(CourseRequest request) {
        if (courses.existsByCodeIgnoreCase(request.code())) {
            throw new ConflictException("A module already uses the code " + request.code());
        }
        Course course = new Course(request.code(), request.title(), request.description(),
                request.level(), request.color());
        return courseMapper.toDetail(courses.save(course), Set.of());
    }

    @Override
    @Transactional
    public CourseDetailResponse update(Long id, CourseRequest request) {
        Course course = courses.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        if (courses.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new ConflictException("A module already uses the code " + request.code());
        }
        course.setCode(request.code());
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setLevel(request.level());
        course.setColor(request.color());
        return courseMapper.toDetail(course, Set.of());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = courses.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        courses.delete(course);
    }

    private CourseSummaryResponse toSummary(Course course, User me, Map<Long, Long> chapterCounts,
                                            Map<Long, Long> lessonCounts, Map<Long, Long> completionCounts) {
        long lessons = lessonCounts.getOrDefault(course.getId(), 0L);
        long done = completionCounts.getOrDefault(course.getId(), 0L);

        Integer progress = null;
        Integer classAverage = null;
        if (me.isAdmin()) {
            long studentsAtLevel = users.countByRoleAndLevelAndStatus(
                    Role.STUDENT, course.getLevel(), UserStatus.ACTIVE);
            classAverage = percent(done, lessons * studentsAtLevel);
        } else {
            progress = percent(done, lessons);
        }

        return courseMapper.toSummary(course, chapterCounts.getOrDefault(course.getId(), 0L),
                lessons, progress, classAverage);
    }

    /** A module with no lessons, or a level with no students, is 0 % rather than a division by zero. */
    private static int percent(long done, long total) {
        return total <= 0 ? 0 : Math.toIntExact(Math.round(100.0 * done / total));
    }

    private static void requireVisible(User me, Course course) {
        if (!me.isAdmin() && !course.getLevel().equals(me.getLevel())) {
            throw new AccessDeniedException("This module belongs to another level");
        }
    }

    private static Map<Long, Long> totals(List<CourseCount> counts) {
        return counts.stream().collect(Collectors.toMap(CourseCount::getCourseId, CourseCount::getTotal));
    }
}
