package com.loyaltyos.support.exception;

public class SupportException extends RuntimeException {

    private final String code;

    public SupportException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
