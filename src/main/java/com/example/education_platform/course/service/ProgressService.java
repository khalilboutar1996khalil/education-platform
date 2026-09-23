package com.example.education_platform.course.service;

public interface ProgressService {

    /** Idempotent: marking an already-finished lesson changes nothing and still succeeds. */
    void markDone(Long lessonId);

    void unmarkDone(Long lessonId);
}
