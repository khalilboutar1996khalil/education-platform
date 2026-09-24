package com.example.education_platform.grade.listener;

import com.example.education_platform.assignment.event.SubmissionGraded;
import com.example.education_platform.grade.service.GradebookService;
import com.example.education_platform.quiz.event.QuizAttemptGraded;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns "a mark was awarded" into a gradebook row.
 *
 * <p>Listening rather than being called keeps the dependency one-way: the gradebook knows about
 * quizzes and assignments, and neither of them knows about the gradebook.
 *
 * <p>Plain {@code @EventListener}, not {@code AFTER_COMMIT}: the gradebook row belongs in the same
 * transaction as the mark that caused it, so a failure here rolls the marking back rather than
 * leaving the two out of step.
 */
@Component
@RequiredArgsConstructor
public class GradeRecorder {

    private final GradebookService gradebook;

    @EventListener
    public void on(QuizAttemptGraded event) {
        gradebook.recordQuizGrade(event.attemptId());
    }

    @EventListener
    public void on(SubmissionGraded event) {
        gradebook.recordAssignmentGrade(event.submissionId());
    }
}
