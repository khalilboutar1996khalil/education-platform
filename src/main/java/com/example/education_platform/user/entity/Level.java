package com.example.education_platform.user.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import java.util.Objects;

/**
 * Reference of a school level (a class year), e.g. {@code SECOND_AS} or {@code 1AS}.
 *
 * <p>Levels used to be a fixed enum; they now live in the {@code school_levels} table so an admin
 * can add one without a release. This value type is only the reference: it is what every
 * {@code level} column stores and what the API sends, so the JSON keeps the shape it had as an
 * enum. The name shown to people lives on {@link com.example.education_platform.level.entity.SchoolLevel}.
 *
 * <p>Compare with {@code equals}, never {@code ==}: two references to the same level are two objects.
 */
public record Level(String code) {

    /** The levels a fresh database starts with (migration V17); kept as constants for seeds and tests. */
    public static final Level SECOND_AS = new Level("SECOND_AS");
    public static final Level THIRD_AS = new Level("THIRD_AS");
    public static final Level FOURTH_AS = new Level("FOURTH_AS");
    public static final Level SEVENTH_BASE = new Level("SEVENTH_BASE");
    public static final Level EIGHTH_BASE = new Level("EIGHTH_BASE");
    public static final Level NINTH_BASE = new Level("NINTH_BASE");

    public Level {
        Objects.requireNonNull(code, "code");
        code = normalise(code);
    }

    /** Typed by hand in the admin screen, so case and stray spaces cannot matter. */
    public static String normalise(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    @JsonCreator
    public static Level of(String code) {
        return code == null || code.isBlank() ? null : new Level(code);
    }

    @JsonValue
    @Override
    public String code() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
