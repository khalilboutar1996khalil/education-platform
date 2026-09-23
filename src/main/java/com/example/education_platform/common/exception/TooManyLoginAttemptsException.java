package com.example.education_platform.common.exception;

import java.time.Duration;

/** Too many consecutive failed logins for one address. Maps to 429 with a Retry-After header. */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final transient Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many failed login attempts; retry in " + retryAfter.toSeconds() + "s");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
