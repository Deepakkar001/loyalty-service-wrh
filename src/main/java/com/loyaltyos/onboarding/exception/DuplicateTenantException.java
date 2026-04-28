package com.loyaltyos.onboarding.exception;

public class DuplicateTenantException extends RuntimeException {
    public DuplicateTenantException(String field, String value) {
        super("A tenant already exists with " + field + ": " + value);
    }
}

