package com.example.education_platform.quiz.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record QuizRequest(
        @NotBlank(message = "{quiz.title.required}")
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description,

        /** Null means untimed. */
        @Positive(message = "{quiz.duration.invalid}")
        Integer durationMinutes,

        Instant opensAt,

        Instant deadline,

        /** Wrapper types, not primitives: a client may leave these out, and Jackson refuses to
         * read a record whose primitive component is absent. Defaults are applied server-side. */
        @Min(value = 1, message = "{quiz.attempts.invalid}")
        Integer maxAttempts,

        Boolean shuffleQuestions) {

    public int maxAttemptsOrDefault() {
        return maxAttempts == null ? 1 : maxAttempts;
    }

    public boolean shuffleQuestionsOrDefault() {
        return Boolean.TRUE.equals(shuffleQuestions);
    }
}
