package com.example.education_platform.common.exception;

/** The request conflicts with existing state (e.g. duplicate email). Maps to 409 Conflict. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
