package com.loyaltyos.rules.exception;

public class RuleEngineConflictException extends RuntimeException {

    private final String errorCode;

    public RuleEngineConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
