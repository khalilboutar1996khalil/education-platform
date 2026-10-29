package com.example.education_platform.coupon.dto.response;

import java.time.Instant;

/**
 * One coupon as the admin sees it, secret included: this is what the print PDF is built from.
 *
 * @param redeemedByName     null while unused
 * @param redeemedByUsername null while unused
 */
public record CouponResponse(
        Long id,
        String serial,
        String code,
        boolean redeemed,
        Instant redeemedAt,
        String redeemedByName,
        String redeemedByUsername) {
}
