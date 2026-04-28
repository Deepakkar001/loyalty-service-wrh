package com.loyaltyos.onboarding.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class IsoCountryCodeValidator implements ConstraintValidator<IsoCountryCode, String> {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries()).stream()
        .map(String::toUpperCase)
        .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank handles required
        String cc = value.trim().toUpperCase();
        return cc.length() == 2 && ISO_COUNTRIES.contains(cc);
    }
}

