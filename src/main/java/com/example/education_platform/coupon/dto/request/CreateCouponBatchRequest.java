package com.example.education_platform.coupon.dto.request;

import com.example.education_platform.level.validation.KnownLevel;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param quantity how many copies of the book are printed, one coupon each */
public record CreateCouponBatchRequest(
        @NotNull(message = "{user.level.required}")
        @KnownLevel
        Level level,

        @NotNull(message = "{coupon.quantity.required}")
        @Min(value = 1, message = "{coupon.quantity.range}")
        @Max(value = 2000, message = "{coupon.quantity.range}")
        Integer quantity,

        @Size(max = 255)
        String label) {
}
