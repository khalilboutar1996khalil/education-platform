package com.example.education_platform.quiz.service;

import com.example.education_platform.common.PageResponse;
import com.example.education_platform.quiz.dto.request.SaveAnswersRequest;
import com.example.education_platform.quiz.dto.response.AttemptResponse;
import com.example.education_platform.quiz.dto.response.StudentQuizResponse;
import org.springframework.data.domain.Pageable;

/** The student's side: taking a quiz. Drafts and answer keys never reach any of these. */
public interface AttemptService {

    PageResponse<StudentQuizResponse> listMyQuizzes(Long courseId, Pageable pageable);

    /** Idempotent: an attempt already in progress is resumed rather than a second one started. */
    AttemptResponse start(Long quizId);

    AttemptResponse get(Long attemptId);

    AttemptResponse saveAnswers(Long attemptId, SaveAnswersRequest request);

    AttemptResponse submit(Long attemptId);
}
