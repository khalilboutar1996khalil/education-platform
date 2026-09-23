package com.example.education_platform.quiz.controller;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.quiz.dto.request.SaveAnswersRequest;
import com.example.education_platform.quiz.dto.response.AttemptResponse;
import com.example.education_platform.quiz.dto.response.StudentQuizResponse;
import com.example.education_platform.quiz.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The student's side. Nothing here can expose a draft quiz or an answer key. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quiz attempts", description = "Taking a quiz (student)")
public class AttemptController {

    private final AttemptService attemptService;

    @GetMapping("/my/quizzes")
    @Operation(summary = "The quizzes open to me, with how many attempts I have used")
    PageResponse<StudentQuizResponse> myQuizzes(
            @RequestParam(required = false) Long courseId,
            @ParameterObject @PageableDefault(size = 20, sort = "deadline", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return attemptService.listMyQuizzes(courseId, pageable);
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @Operation(summary = "Start a quiz, or resume the attempt already in progress")
    AttemptResponse start(@PathVariable Long quizId) {
        return attemptService.start(quizId);
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "Read one of my attempts")
    AttemptResponse get(@PathVariable Long attemptId) {
        return attemptService.get(attemptId);
    }

    @PutMapping("/attempts/{attemptId}/answers")
    @Operation(summary = "Save answers; send only the questions that changed")
    AttemptResponse saveAnswers(@PathVariable Long attemptId, @Valid @RequestBody SaveAnswersRequest request) {
        return attemptService.saveAnswers(attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Hand in the attempt; choice questions are scored immediately")
    AttemptResponse submit(@PathVariable Long attemptId) {
        return attemptService.submit(attemptId);
    }
}
