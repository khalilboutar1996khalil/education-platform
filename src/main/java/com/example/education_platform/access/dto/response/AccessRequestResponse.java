package com.example.education_platform.access.dto.response;

import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.subscription.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param createdUser the account an approval produced (or renewed); null while pending or once refused
 * @param price       what the plan cost when the student asked; null for a free request
 * @param receiptName filename of the attached receipt, downloaded from {@code /{id}/receipt}; null if none
 * @param renewal     an existing student extending their access rather than a new account
 */
public record AccessRequestResponse(
        Long id,
        String fullName,
        String email,
        Level level,
        String message,
        AccessRequestStatus status,
        UserResponse reviewedBy,
        Instant reviewedAt,
        UserResponse createdUser,
        String decisionNote,
        Instant submittedAt,
        BigDecimal price,
        Integer durationDays,
        PaymentMethod paymentMethod,
        String paymentReference,
        String receiptName,
        boolean renewal) {
}
