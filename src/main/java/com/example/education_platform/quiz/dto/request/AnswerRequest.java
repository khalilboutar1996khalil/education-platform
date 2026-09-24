package com.example.education_platform.quiz.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** @param selectedChoiceIds empty for an open question; the text goes in {@code textAnswer} */
public record AnswerRequest(
        @NotNull(message = "{answer.question.required}")
        Long questionId,

        List<Long> selectedChoiceIds,

        @Size(max = 5000)
        String textAnswer) {

    public List<Long> selectedChoiceIdsOrEmpty() {
        return selectedChoiceIds == null ? List.of() : selectedChoiceIds;
    }
}
