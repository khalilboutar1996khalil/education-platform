package com.example.education_platform.subscription.entity;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

/** Dates of the Tunisian school year, which runs from September to the end of June. */
public final class SchoolYear {

    /** Access ends are days in Tunisia, whatever the server's own zone. */
    public static final ZoneId ZONE = ZoneId.of("Africa/Tunis");

    private SchoolYear() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** 30 June of the school year that {@code day} belongs to (from July on, the next one). */
    public static LocalDate endOf(LocalDate day) {
        int year = day.getMonthValue() >= Month.JULY.getValue() ? day.getYear() + 1 : day.getYear();
        return LocalDate.of(year, Month.JUNE, 30);
    }
}
