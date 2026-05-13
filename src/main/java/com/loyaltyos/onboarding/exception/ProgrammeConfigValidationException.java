package com.loyaltyos.onboarding.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProgrammeConfigValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ProgrammeConfigValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

