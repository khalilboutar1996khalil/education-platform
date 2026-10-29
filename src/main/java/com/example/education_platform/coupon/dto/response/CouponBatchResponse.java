package com.example.education_platform.coupon.dto.response;

import com.example.education_platform.user.entity.Level;
import java.time.Instant;

/** @param redeemed how many of the batch's coupons have been used so far */
public record CouponBatchResponse(
        Long id,
        Level level,
        int quantity,
        String label,
        long redeemed,
        Instant createdAt) {
}
