package com.example.education_platform.subscription.controller;

import com.example.education_platform.subscription.dto.PaymentSettingsDto;
import com.example.education_platform.subscription.dto.request.AccessUntilRequest;
import com.example.education_platform.subscription.dto.request.PlanRequest;
import com.example.education_platform.subscription.dto.response.OfferResponse;
import com.example.education_platform.subscription.dto.response.PlanResponse;
import com.example.education_platform.subscription.service.SubscriptionService;
import com.example.education_platform.user.dto.response.UserResponse;
import com.example.education_platform.user.entity.Level;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Abonnements", description = "Yearly plans per level, and where students pay")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/offer")
    @SecurityRequirements // public: shown on the form of somebody who has no account yet
    @Operation(summary = "The plans on sale and the payment details, for the public form")
    OfferResponse offer() {
        return subscriptionService.offer();
    }

    @GetMapping("/plans")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Every plan, active or not")
    List<PlanResponse> plans() {
        return subscriptionService.plans();
    }

    @PutMapping("/plans/{level}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or change the yearly plan of a level")
    PlanResponse savePlan(@PathVariable String level, @Valid @RequestBody PlanRequest request) {
        return subscriptionService.savePlan(Level.of(level), request);
    }

    @GetMapping("/payment-settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Where students send the money")
    PaymentSettingsDto paymentSettings() {
        return subscriptionService.paymentSettings();
    }

    @PutMapping("/payment-settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change the payment details shown on the public form")
    PaymentSettingsDto savePaymentSettings(@Valid @RequestBody PaymentSettingsDto request) {
        return subscriptionService.savePaymentSettings(request);
    }

    @PutMapping("/students/{id}/access-until")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set the last day a student may sign in (extend or shorten by hand)")
    UserResponse setAccessUntil(@PathVariable Long id, @Valid @RequestBody AccessUntilRequest request) {
        return subscriptionService.setAccessUntil(id, request.accessUntil());
    }
}
