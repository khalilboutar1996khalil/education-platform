package com.example.education_platform.coupon.dto.response;

import com.example.education_platform.user.entity.Level;
import java.time.Instant;

/**
 * A book activation waiting for the admin's decision.
 *
 * @param couponId    what to accept or refuse
 * @param batchLabel  the batch's title, e.g. "Cours de 7e année"; may be null
 * @param requestedAt when the student activated the book
 */
public record PendingActivationResponse(
        Long couponId,
        String serial,
        Long batchId,
        String batchLabel,
        Level level,
        String studentName,
        String username,
        Instant requestedAt) {
}
