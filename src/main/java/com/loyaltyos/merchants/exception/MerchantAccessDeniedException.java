package com.loyaltyos.merchants.exception;

public class MerchantAccessDeniedException extends RuntimeException {
    public MerchantAccessDeniedException(String message) {
        super(message);
    }
}
