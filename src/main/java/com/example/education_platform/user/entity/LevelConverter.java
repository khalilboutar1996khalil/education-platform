package com.example.education_platform.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a {@link Level} as its reference, so every {@code level} column stays a plain VARCHAR. */
@Converter(autoApply = true)
public class LevelConverter implements AttributeConverter<Level, String> {

    @Override
    public String convertToDatabaseColumn(Level level) {
        return level == null ? null : level.code();
    }

    @Override
    public Level convertToEntityAttribute(String code) {
        return code == null ? null : new Level(code);
    }
}
