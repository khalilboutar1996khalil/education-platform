package com.example.education_platform.subscription.dto.response;

import com.example.education_platform.user.entity.Level;
import java.math.BigDecimal;

/** @param levelName what people read, e.g. "7ᵉ année de base informatique" */
public record PlanResponse(
        Long id,
        Level level,
        String levelName,
        BigDecimal price,
        int durationDays,
        boolean active) {
}
