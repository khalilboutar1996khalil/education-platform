package com.example.education_platform.auth.service;

import com.example.education_platform.auth.dto.request.LoginRequest;
import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.auth.dto.response.TokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String userAgent, String ipAddress);

    TokenResponse refresh(String refreshToken, String userAgent, String ipAddress);

    void logout(String refreshToken);
}
