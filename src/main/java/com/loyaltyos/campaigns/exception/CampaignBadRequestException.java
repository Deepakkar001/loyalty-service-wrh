package com.loyaltyos.campaigns.exception;

import java.util.Collections;
import java.util.Map;

public class CampaignBadRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public CampaignBadRequestException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public CampaignBadRequestException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
}
