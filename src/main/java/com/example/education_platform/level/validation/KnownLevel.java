package com.example.education_platform.level.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The level must exist and be active. Null passes: pair with {@code @NotNull} where it is required. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = KnownLevelValidator.class)
public @interface KnownLevel {

    String message() default "{level.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
