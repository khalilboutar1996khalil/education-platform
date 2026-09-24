package com.example.education_platform.quiz.event;

/**
 * Raised when an attempt has been scored. Published rather than calling the gradebook directly:
 * the quiz feature must not depend on the gradebook, or the two packages would depend on each
 * other. Step 9 turns this into the wider domain-event mechanism.
 */
public record QuizAttemptGraded(Long attemptId) {
}
