package com.example.education_platform.assignment.entity;

public enum SubmissionStatus {
    /** Started but not handed in; the student can still change it. */
    DRAFT,
    SUBMITTED,
    /** Handed in after the deadline, which the teacher sees when marking. */
    LATE,
    GRADED
}
