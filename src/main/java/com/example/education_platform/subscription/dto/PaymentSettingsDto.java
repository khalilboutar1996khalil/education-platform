package com.example.education_platform.subscription.dto;

import jakarta.validation.constraints.Size;

/** Where students send the money. Every field is optional: the admin fills what they use. */
public record PaymentSettingsDto(
        @Size(max = 40) String d17Number,
        @Size(max = 120) String bankName,
        @Size(max = 40) String rib,
        @Size(max = 120) String accountHolder,
        @Size(max = 1000) String instructions) {
}
