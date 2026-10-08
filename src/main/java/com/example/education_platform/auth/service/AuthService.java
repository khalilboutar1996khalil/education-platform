package com.example.education_platform.auth.service;

import com.example.education_platform.auth.dto.request.LoginRequest;
import com.example.education_platform.auth.dto.request.RegisterRequest;
import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.auth.dto.response.TokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String userAgent, String ipAddress);

    /**
     * Creates an active student account at the level they picked and signs them in straight away:
     * there is no approval step and no email, so the tokens come back with this same call.
     */
    LoginResponse register(RegisterRequest request, String userAgent, String ipAddress);

    TokenResponse refresh(String refreshToken, String userAgent, String ipAddress);

    void logout(String refreshToken);

    /**
     * Always succeeds, whether or not the address exists: answering differently would turn this
     * endpoint into a way of discovering who has an account.
     */
    void requestPasswordReset(String email);

    /** Consumes the token, sets the password and ends every session opened with the old one. */
    void resetPassword(String token, String newPassword);
}
