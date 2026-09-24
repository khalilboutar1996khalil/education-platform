package com.example.education_platform.assignment.dto.request;

import com.example.education_platform.assignment.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;

public record AssignmentStatusRequest(
        @NotNull(message = "{assignment.status.required}") AssignmentStatus status) {
}
