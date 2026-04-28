package com.loyaltyos.onboarding.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IsoCountryCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCountryCode {
    String message() default "Country code must be a valid ISO 3166-1 alpha-2 code (e.g. IN, US)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

