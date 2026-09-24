package com.example.education_platform.auth.dto.response;

import com.example.education_platform.user.dto.response.UserResponse;

/** Login returns the tokens plus the profile, so the client needs no follow-up call to render. */
public record LoginResponse(TokenResponse tokens, UserResponse user) {
}
