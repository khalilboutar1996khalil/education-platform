package com.example.education_platform.quiz.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.quiz.dto.request.QuestionRequest;
import com.example.education_platform.quiz.dto.request.QuizRequest;
import com.example.education_platform.quiz.dto.request.QuizStatusRequest;
import com.example.education_platform.quiz.dto.response.QuizDetailResponse;
import com.example.education_platform.quiz.dto.response.QuizSummaryResponse;
import com.example.education_platform.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authoring only. Everything here shows the answer key, so it is admin-only without exception. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Quiz authoring", description = "Writing quizzes, questions and choices (admin)")
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/quizzes")
    @Operation(summary = "List quizzes, drafts included")
    PageResponse<QuizSummaryResponse> list(
            @RequestParam(required = false) Long courseId,
            @ParameterObject @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return quizService.list(courseId, pageable);
    }

    @GetMapping("/quizzes/{id}")
    @Operation(summary = "One quiz with its questions, the answer key and submission stats")
    QuizDetailResponse getDetail(@PathVariable Long id) {
        return quizService.getDetail(id);
    }

    @PostMapping("/courses/{courseId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz as a draft")
    QuizDetailResponse create(@PathVariable Long courseId, @Valid @RequestBody QuizRequest request) {
        return quizService.create(courseId, request);
    }

    @PutMapping("/quizzes/{id}")
    @Operation(summary = "Update a quiz's settings")
    QuizDetailResponse update(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, request);
    }

    @PatchMapping("/quizzes/{id}/status")
    @Operation(summary = "Open or close a quiz; opening requires at least one question")
    QuizDetailResponse changeStatus(@PathVariable Long id, @Valid @RequestBody QuizStatusRequest request) {
        return quizService.changeStatus(id, request.status());
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a quiz with its questions and attempts")
    void delete(@PathVariable Long id) {
        quizService.delete(id);
    }

    @PostMapping("/quizzes/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a question with its choices; only while the quiz is a draft")
    QuizDetailResponse addQuestion(@PathVariable Long quizId, @Valid @RequestBody QuestionRequest request) {
        return quizService.addQuestion(quizId, request);
    }

    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Update a question; the choices sent replace the existing ones")
    QuizDetailResponse updateQuestion(@PathVariable Long questionId, @Valid @RequestBody QuestionRequest request) {
        return quizService.updateQuestion(questionId, request);
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a question; the remaining ones are renumbered")
    QuizDetailResponse deleteQuestion(@PathVariable Long questionId) {
        return quizService.deleteQuestion(questionId);
    }
}
