package com.loyaltyos.coupon.support;

public final class CouponCodeNormalizer {

    private CouponCodeNormalizer() {}

    public static String normalize(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase();
    }
}
