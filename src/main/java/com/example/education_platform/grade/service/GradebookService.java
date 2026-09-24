package com.example.education_platform.grade.service;

import com.example.education_platform.grade.dto.request.ManualGradeRequest;
import com.example.education_platform.grade.dto.response.CourseAverageResponse;
import com.example.education_platform.grade.dto.response.GradeResponse;
import com.example.education_platform.grade.dto.response.GradebookRowResponse;
import java.util.List;

public interface GradebookService {

    // --- recording, driven by events from the quiz and assignment features ---

    /** Idempotent: re-marking updates the existing row rather than adding a second mark. */
    void recordQuizGrade(Long attemptId);

    void recordAssignmentGrade(Long submissionId);

    // --- the teacher's side ---

    GradebookRowResponse addManualGrade(Long courseId, ManualGradeRequest request);

    /** Every student holding a mark in this module, with their marks and their average. */
    List<GradebookRowResponse> gradebook(Long courseId);

    void deleteManualGrade(Long gradeId);

    // --- the student's side ---

    List<CourseAverageResponse> myAverages();

    List<GradeResponse> myGrades(Long courseId);

    /** One number across every module, for the dashboard. Null when nothing has been marked. */
    java.math.BigDecimal overallAverage(Long studentId);
}
