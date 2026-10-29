package com.example.education_platform.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param serial the number printed next to the QR code; the QR code fills it in
 * @param code   the secret under the scratch-off panel
 */
public record RedeemCouponRequest(
        @NotBlank(message = "{coupon.serial.required}")
        @Size(max = 20)
        String serial,

        @NotBlank(message = "{coupon.code.required}")
        @Size(max = 20)
        String code,

        @NotBlank(message = "{coupon.firstName.required}")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "{coupon.lastName.required}")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "{user.password.required}")
        @Size(min = 10, max = 100, message = "{user.password.tooShort}")
        String password) {
}
