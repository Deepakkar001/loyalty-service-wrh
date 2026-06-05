package com.loyaltyos.campaigns.exception;

public class CampaignConflictException extends RuntimeException {

    private final String errorCode;

    public CampaignConflictException(String message) {
        this(null, message);
    }

    public CampaignConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
