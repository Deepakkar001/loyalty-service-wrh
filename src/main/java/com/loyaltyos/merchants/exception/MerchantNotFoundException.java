package com.loyaltyos.merchants.exception;

public class MerchantNotFoundException extends RuntimeException {
    public MerchantNotFoundException(String merchantUid) {
        super("Merchant not found: " + merchantUid);
    }
}
