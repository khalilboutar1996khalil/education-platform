package com.example.education_platform.quiz.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Answers are upserted, so a client may send only the questions the student just touched. */
public record SaveAnswersRequest(
        @NotEmpty(message = "{answer.list.required}")
        @Valid
        List<AnswerRequest> answers) {
}
