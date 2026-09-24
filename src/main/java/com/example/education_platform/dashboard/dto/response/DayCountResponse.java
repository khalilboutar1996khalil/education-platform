package com.example.education_platform.dashboard.dto.response;

import java.time.LocalDate;

/** One bar of the weekly chart. Days with nothing are present with a zero, not missing. */
public record DayCountResponse(LocalDate day, long count) {
}
