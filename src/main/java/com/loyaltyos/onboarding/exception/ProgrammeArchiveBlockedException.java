package com.loyaltyos.onboarding.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when a programme cannot be archived (soft-deleted) due to business constraints.
 */
public class ProgrammeArchiveBlockedException extends RuntimeException {

    private final Map<String, String> reasons;

    public ProgrammeArchiveBlockedException(String message, Map<String, String> reasons) {
        super(message);
        this.reasons = reasons == null ? Map.of() : Map.copyOf(reasons);
    }

    public static ProgrammeArchiveBlockedException single(String key, String message) {
        Map<String, String> reasons = new LinkedHashMap<>();
        reasons.put(key, message);
        return new ProgrammeArchiveBlockedException(message, reasons);
    }

    public Map<String, String> getReasons() {
        return reasons;
    }
}
