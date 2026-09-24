package com.example.education_platform.notification.entity;

public enum NotificationType {
    ANNOUNCEMENT,
    /** A submission has been marked. */
    GRADED,
    /** A quiz attempt has been scored. */
    QUIZ_SCORED,
    /** A deadline is close; raised by the scheduled reminder. */
    DEADLINE
}
