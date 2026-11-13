package com.example.education_platform.subscription.service;

import com.example.education_platform.subscription.dto.PaymentSettingsDto;
import com.example.education_platform.subscription.dto.request.PlanRequest;
import com.example.education_platform.subscription.dto.response.OfferResponse;
import com.example.education_platform.subscription.dto.response.PlanResponse;
import com.example.education_platform.subscription.entity.SubscriptionPlan;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionService {

    /** Public: the active plans and the payment details. */
    OfferResponse offer();

    /** Admin: every plan, active or not. */
    List<PlanResponse> plans();

    /** Admin: creates the plan of a level, or changes it. */
    PlanResponse savePlan(Level level, PlanRequest request);

    PaymentSettingsDto paymentSettings();

    PaymentSettingsDto savePaymentSettings(PaymentSettingsDto request);

    /** Admin: sets the last day a student may sign in, e.g. to extend someone by hand. */
    UserResponse setAccessUntil(Long studentId, LocalDate accessUntil);

    /** The plan a request for this level must pay for, if the level has one. */
    Optional<SubscriptionPlan> activePlan(Level level);
}
