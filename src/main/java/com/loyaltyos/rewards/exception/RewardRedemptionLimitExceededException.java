package com.loyaltyos.rewards.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RewardRedemptionLimitExceededException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public RewardRedemptionLimitExceededException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : new LinkedHashMap<>(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
}
