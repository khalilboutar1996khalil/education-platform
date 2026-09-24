package com.example.education_platform.quiz.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.quiz.dto.request.QuestionRequest;
import com.example.education_platform.quiz.dto.request.QuizRequest;
import com.example.education_platform.quiz.dto.response.QuizDetailResponse;
import com.example.education_platform.quiz.dto.response.QuizSummaryResponse;
import com.example.education_platform.quiz.entity.QuizStatus;
import org.springframework.data.domain.Pageable;

/** Quiz authoring. Everything here is the admin's side; students never reach these methods. */
public interface QuizService {

    PageResponse<QuizSummaryResponse> list(Long courseId, Pageable pageable);

    QuizDetailResponse getDetail(Long id);

    QuizDetailResponse create(Long courseId, QuizRequest request);

    QuizDetailResponse update(Long id, QuizRequest request);

    QuizDetailResponse changeStatus(Long id, QuizStatus status);

    void delete(Long id);

    QuizDetailResponse addQuestion(Long quizId, QuestionRequest request);

    QuizDetailResponse updateQuestion(Long questionId, QuestionRequest request);

    QuizDetailResponse deleteQuestion(Long questionId);
}
