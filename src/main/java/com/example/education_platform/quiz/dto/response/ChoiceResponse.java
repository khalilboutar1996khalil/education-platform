package com.example.education_platform.quiz.dto.response;

/**
 * Admin-facing, and the only response type that carries {@code correct}. The student attempt view
 * uses its own type, so the answer key cannot leak by someone adding a field in the wrong place.
 */
public record ChoiceResponse(Long id, int position, String text, boolean correct) {
}
