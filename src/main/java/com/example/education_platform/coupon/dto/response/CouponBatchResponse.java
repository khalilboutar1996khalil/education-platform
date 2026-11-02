package com.example.education_platform.coupon.dto.response;

import com.example.education_platform.user.entity.Level;
import java.time.Instant;

/**
 * @param redeemed activations the admin accepted
 * @param pending  activations still waiting for the admin's decision
 */
public record CouponBatchResponse(
        Long id,
        Level level,
        int quantity,
        String label,
        long redeemed,
        long pending,
        Instant createdAt) {
}
