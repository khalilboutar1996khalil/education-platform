package com.example.education_platform.coupon.dto.response;

import com.example.education_platform.user.entity.Level;

/**
 * What a student learns after activating a book. No session: the account waits for the admin.
 *
 * @param username what they will sign in with once accepted
 */
public record RedeemCouponResponse(String fullName, String username, Level level) {
}
