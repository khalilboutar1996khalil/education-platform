package com.example.education_platform.grade.service.impl;

import com.example.education_platform.assignment.entity.Submission;
import com.example.education_platform.assignment.entity.SubmissionStatus;
import com.example.education_platform.assignment.repository.SubmissionRepository;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ResourceNotFoundException;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.grade.dto.request.ManualGradeRequest;
import com.example.education_platform.grade.dto.response.CourseAverageResponse;
import com.example.education_platform.grade.dto.response.GradeResponse;
import com.example.education_platform.grade.dto.response.GradebookRowResponse;
import com.example.education_platform.grade.entity.Grade;
import com.example.education_platform.grade.entity.GradeKind;
import com.example.education_platform.grade.mapper.GradeMapper;
import com.example.education_platform.grade.repository.GradeRepository;
import com.example.education_platform.grade.service.GradebookService;
import com.example.education_platform.quiz.entity.AttemptStatus;
import com.example.education_platform.quiz.entity.QuizAttempt;
import com.example.education_platform.quiz.repository.QuizAttemptRepository;
import com.example.education_platform.security.CurrentUser;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.mapper.UserMapper;
import com.example.education_platform.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GradebookServiceImpl implements GradebookService {

    private final GradeRepository grades;
    private final QuizAttemptRepository attempts;
    private final SubmissionRepository submissions;
    private final CourseRepository courses;
    private final UserRepository users;
    private final GradeMapper gradeMapper;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    // ---------- recording ----------

    @Override
    public void recordQuizGrade(Long attemptId) {
        QuizAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));
        // An attempt still awaiting manual marking has no final score to publish yet
        if (attempt.getStatus() != AttemptStatus.GRADED || attempt.getScore() == null) {
            return;
        }

        Grade grade = grades.findByQuizAttemptId(attemptId).orElseGet(() -> {
            Grade fresh = new Grade(attempt.getStudent(), attempt.getQuiz().getCourse(),
                    GradeKind.QUIZ, attempt.getQuiz().getTitle(),
                    attempt.getScore(), attempt.getMaxScore());
            fresh.setQuizAttempt(attempt);
            return grades.save(fresh);
        });
        grade.setScore(attempt.getScore());
        grade.setMaxScore(attempt.getMaxScore());
        grade.setLabel(attempt.getQuiz().getTitle());
    }

    @Override
    public void recordAssignmentGrade(Long submissionId) {
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
        if (submission.getStatus() != SubmissionStatus.GRADED || submission.getGrade() == null) {
            return;
        }

        Grade grade = grades.findBySubmissionId(submissionId).orElseGet(() -> {
            Grade fresh = new Grade(submission.getStudent(), submission.getAssignment().getCourse(),
                    GradeKind.ASSIGNMENT, submission.getAssignment().getTitle(),
                    submission.getGrade(), submission.getAssignment().getMaxPoints());
            fresh.setSubmission(submission);
            return grades.save(fresh);
        });
        grade.setScore(submission.getGrade());
        grade.setMaxScore(submission.getAssignment().getMaxPoints());
        grade.setLabel(submission.getAssignment().getTitle());

        // A pair shares the work, so it shares the mark: the partner gets their own row
        if (submission.getPartner() != null) {
            recordPartnerGrade(submission);
        }
    }

    /**
     * The partner's row cannot point at the same submission — the partial unique index allows one
     * grade per source — so it is recorded as a manual entry carrying the same numbers.
     */
    private void recordPartnerGrade(Submission submission) {
        User partner = submission.getPartner();
        String label = submission.getAssignment().getTitle();
        boolean alreadyThere = grades
                .findByCourseIdAndStudentIdOrderByCreatedAtAsc(
                        submission.getAssignment().getCourse().getId(), partner.getId())
                .stream()
                .anyMatch(existing -> existing.getLabel().equals(label));

        if (!alreadyThere) {
            grades.save(new Grade(partner, submission.getAssignment().getCourse(),
                    GradeKind.MANUAL, label, submission.getGrade(),
                    submission.getAssignment().getMaxPoints()));
        }
    }

    // ---------- the teacher's side ----------

    @Override
    public GradebookRowResponse addManualGrade(Long courseId, ManualGradeRequest request) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        User student = users.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.studentId()));

        if (student.isAdmin()) {
            throw new BusinessException("Only a student can be graded");
        }
        if (student.getLevel() != course.getLevel()) {
            throw new BusinessException("This student does not follow this module");
        }
        if (request.score().compareTo(request.maxScore()) > 0) {
            throw new BusinessException("The score cannot exceed the maximum");
        }

        Grade grade = new Grade(student, course, GradeKind.MANUAL, request.label(),
                request.score(), request.maxScore());
        grade.setWeight(request.weightOrDefault());
        grade.setRecordedBy(currentUser.get());
        grades.save(grade);
        grades.flush();

        return rowFor(student, grades.findByCourseIdAndStudentIdOrderByCreatedAtAsc(courseId, student.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradebookRowResponse> gradebook(Long courseId) {
        if (!courses.existsById(courseId)) {
            throw new ResourceNotFoundException("Course", courseId);
        }
        return grades.findByCourseIdOrderByStudentIdAsc(courseId).stream()
                .collect(Collectors.groupingBy(Grade::getStudent))
                .entrySet().stream()
                .map(entry -> rowFor(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(row -> row.student().fullName()))
                .toList();
    }

    @Override
    public void deleteManualGrade(Long gradeId) {
        Grade grade = grades.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", gradeId));
        if (grade.getKind() != GradeKind.MANUAL) {
            // Deleting it would only invite the source to write it straight back
            throw new BusinessException("An automatic grade is removed by changing the work it came from");
        }
        grades.delete(grade);
    }

    // ---------- the student's side ----------

    @Override
    @Transactional(readOnly = true)
    public List<CourseAverageResponse> myAverages() {
        User me = requireStudent();
        Map<Course, List<Grade>> byCourse = grades.findByStudentIdOrderByCourseIdAscCreatedAtAsc(me.getId())
                .stream()
                .collect(Collectors.groupingBy(Grade::getCourse));

        return byCourse.entrySet().stream()
                .map(entry -> new CourseAverageResponse(
                        entry.getKey().getId(), entry.getKey().getCode(), entry.getKey().getTitle(),
                        weightedAverage(entry.getValue()), entry.getValue().size()))
                .sorted(Comparator.comparing(CourseAverageResponse::courseCode))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> myGrades(Long courseId) {
        User me = requireStudent();
        return gradeMapper.toResponses(
                grades.findByCourseIdAndStudentIdOrderByCreatedAtAsc(courseId, me.getId()));
    }

    // ---------- averaging ----------

    /**
     * Weighted mean of the marks rescaled to /20. Null rather than zero when there is nothing to
     * average: a module with no marks is not a module the student failed.
     */
    private static BigDecimal weightedAverage(List<Grade> marks) {
        if (marks.isEmpty()) {
            return null;
        }
        BigDecimal weightTotal = marks.stream()
                .map(Grade::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (weightTotal.signum() <= 0) {
            return null;
        }
        BigDecimal weighted = marks.stream()
                .map(mark -> mark.outOfTwenty().multiply(mark.getWeight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return weighted.divide(weightTotal, 2, RoundingMode.HALF_UP);
    }

    private GradebookRowResponse rowFor(User student, List<Grade> marks) {
        return new GradebookRowResponse(userMapper.toResponse(student),
                gradeMapper.toResponses(marks), weightedAverage(marks));
    }

    private User requireStudent() {
        User me = currentUser.get();
        if (me.isAdmin()) {
            throw new AccessDeniedException("An admin has no marks of their own");
        }
        return me;
    }
}
