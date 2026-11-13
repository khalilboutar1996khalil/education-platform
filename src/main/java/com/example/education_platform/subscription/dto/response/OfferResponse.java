package com.example.education_platform.subscription.dto.response;

import com.example.education_platform.subscription.dto.PaymentSettingsDto;
import java.util.List;

/** What the public form shows: the active plans, and where to pay. */
public record OfferResponse(List<PlanResponse> plans, PaymentSettingsDto payment) {
}
