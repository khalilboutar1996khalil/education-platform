package com.example.education_platform.common.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * The presented refresh token is unknown, expired or already revoked. Separate from
 * {@code BadCredentialsException} so an expired session isn't reported as a wrong password.
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
