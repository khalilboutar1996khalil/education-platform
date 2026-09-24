package com.example.education_platform.auth.controller;

import com.example.education_platform.auth.dto.request.ForgotPasswordRequest;
import com.example.education_platform.auth.dto.request.LoginRequest;
import com.example.education_platform.auth.dto.request.RefreshRequest;
import com.example.education_platform.auth.dto.request.ResetPasswordRequest;
import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.auth.dto.response.TokenResponse;
import com.example.education_platform.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login, token refresh and logout")
@SecurityRequirements // these three endpoints take no Bearer token, so Swagger shouldn't ask for one
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for an access token and a refresh token")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getHeader("User-Agent"), http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new pair; the presented token is revoked")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return authService.refresh(request.refreshToken(), http.getHeader("User-Agent"), http.getRemoteAddr());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ask for a reset link; answers the same whether or not the address exists")
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password from a reset link; this signs out every session")
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a refresh token")
    void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }
}
