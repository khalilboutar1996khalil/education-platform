package com.example.education_platform.level.validation;

import com.example.education_platform.level.repository.SchoolLevelRepository;
import com.example.education_platform.user.entity.Level;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KnownLevelValidator implements ConstraintValidator<KnownLevel, Level> {

    private final SchoolLevelRepository levels;

    @Override
    public boolean isValid(Level level, ConstraintValidatorContext context) {
        return level == null || levels.existsByCodeAndActiveTrue(level.code());
    }
}
