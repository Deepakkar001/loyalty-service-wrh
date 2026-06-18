package com.loyaltyos.merchants.dto;

/**
 * Result of checking whether an email can be used for merchant onboarding / portal access.
 */
public class MerchantEmailAvailabilityResponse {

    private boolean available;
    private String message;

    public MerchantEmailAvailabilityResponse() {}

    public MerchantEmailAvailabilityResponse(boolean available, String message) {
        this.available = available;
        this.message = message;
    }

    public static MerchantEmailAvailabilityResponse available() {
        return new MerchantEmailAvailabilityResponse(true, null);
    }

    public static MerchantEmailAvailabilityResponse unavailable(String message) {
        return new MerchantEmailAvailabilityResponse(false, message);
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
