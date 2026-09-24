package com.example.education_platform.access.entity;

public enum AccessRequestStatus {
    PENDING,
    /** An account was created; the resulting user is linked on the request. */
    APPROVED,
    REJECTED
}
