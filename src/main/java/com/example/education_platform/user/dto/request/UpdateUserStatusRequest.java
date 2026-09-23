package com.example.education_platform.user.dto.request;

import com.example.education_platform.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "{user.status.required}") UserStatus status) {
}
