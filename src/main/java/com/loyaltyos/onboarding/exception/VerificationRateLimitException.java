package com.loyaltyos.onboarding.exception;

public class VerificationRateLimitException extends RuntimeException {
    public VerificationRateLimitException(String message) {
        super(message);
    }
}

