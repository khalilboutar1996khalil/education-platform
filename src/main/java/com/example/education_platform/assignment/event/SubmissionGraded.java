package com.example.education_platform.assignment.event;

/** Raised when a teacher marks a submission. See {@code QuizAttemptGraded} for why it is an event. */
public record SubmissionGraded(Long submissionId) {
}
