package com.example.education_platform.quiz.dto.response;

/**
 * The student-facing choice. It has no {@code correct} field at all — the answer key cannot leak
 * through this type even by accident, because there is nowhere to put it.
 */
public record AttemptChoiceResponse(Long id, int position, String text) {
}
