package com.example.education_platform.common.exception;

/** A domain rule was violated (e.g. submitting after the deadline). Maps to 422 Unprocessable Entity. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
