package com.example.education_platform.user.entity;

/** School level of the computer-science section. */
public enum Level {
    SECOND_AS("2ᵉ AS informatique"),
    THIRD_AS("3ᵉ AS informatique"),
    FOURTH_AS("4ᵉ AS informatique"),
    SEVENTH_BASE("7ᵉ année de base informatique"),
    EIGHTH_BASE("8ᵉ année de base informatique"),
    NINTH_BASE("9ᵉ année de base informatique");

    private final String label;

    Level(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
