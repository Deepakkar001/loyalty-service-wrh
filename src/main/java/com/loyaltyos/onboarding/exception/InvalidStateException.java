package com.loyaltyos.onboarding.exception;

public class InvalidStateException extends RuntimeException {
    private final String currentStatus;
    private final String requiredStatus;

    public InvalidStateException(String message, String currentStatus, String requiredStatus) {
        super(message);
        this.currentStatus = currentStatus;
        this.requiredStatus = requiredStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getRequiredStatus() {
        return requiredStatus;
    }
}

