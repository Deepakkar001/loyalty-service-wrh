package com.loyaltyos.referrals.exception;

public class ReferralException extends RuntimeException {

    private final String code;

    public ReferralException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
