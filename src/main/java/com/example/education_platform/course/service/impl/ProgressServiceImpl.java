package com.example.education_platform.course.service.impl;

import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonCompletion;
import com.example.education_platform.course.repository.LessonCompletionRepository;
import com.example.education_platform.course.repository.LessonRepository;
import com.example.education_platform.course.service.ProgressService;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgressServiceImpl implements ProgressService {

    private final LessonRepository lessons;
    private final LessonCompletionRepository completions;
    private final CurrentUser currentUser;

    @Override
    public void markDone(Long lessonId) {
        User student = requireStudent();
        Lesson lesson = visibleLesson(lessonId, student);

        // Checked rather than caught: the unique constraint is the safety net, not the control flow
        if (!completions.existsByStudentIdAndLessonId(student.getId(), lessonId)) {
            completions.save(new LessonCompletion(student, lesson));
        }
    }

    @Override
    public void unmarkDone(Long lessonId) {
        User student = requireStudent();
        visibleLesson(lessonId, student);
        completions.deleteByStudentIdAndLessonId(student.getId(), lessonId);
    }

    private User requireStudent() {
        User me = currentUser.get();
        if (me.isAdmin()) {
            throw new AccessDeniedException("Progress belongs to students; an admin has none");
        }
        return me;
    }

    private Lesson visibleLesson(Long lessonId, User student) {
        Lesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
        if (lesson.getChapter().getCourse().getLevel() != student.getLevel()) {
            throw new AccessDeniedException("This lesson belongs to another level");
        }
        return lesson;
    }
}
