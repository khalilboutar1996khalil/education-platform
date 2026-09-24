package com.example.education_platform.grade.controller;

import com.example.education_platform.grade.dto.request.ManualGradeRequest;
import com.example.education_platform.grade.dto.response.CourseAverageResponse;
import com.example.education_platform.grade.dto.response.GradeResponse;
import com.example.education_platform.grade.dto.response.GradebookRowResponse;
import com.example.education_platform.grade.service.GradebookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Not paginated, unlike the other list endpoints: a gradebook is one class in one module, and it
 * is read as a whole sheet rather than scrolled.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Gradebook and module averages")
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping("/courses/{courseId}/gradebook")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Every student's marks and average in this module")
    List<GradebookRowResponse> gradebook(@PathVariable Long courseId) {
        return gradebookService.gradebook(courseId);
    }

    @PostMapping("/courses/{courseId}/grades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a mark by hand, e.g. a partiel")
    GradebookRowResponse addManualGrade(@PathVariable Long courseId,
                                        @Valid @RequestBody ManualGradeRequest request) {
        return gradebookService.addManualGrade(courseId, request);
    }

    @DeleteMapping("/grades/{gradeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a hand-entered mark")
    void deleteManualGrade(@PathVariable Long gradeId) {
        gradebookService.deleteManualGrade(gradeId);
    }

    @GetMapping("/my/averages")
    @Operation(summary = "My average in each module")
    List<CourseAverageResponse> myAverages() {
        return gradebookService.myAverages();
    }

    @GetMapping("/my/grades/{courseId}")
    @Operation(summary = "My marks in one module")
    List<GradeResponse> myGrades(@PathVariable Long courseId) {
        return gradebookService.myGrades(courseId);
    }
}
