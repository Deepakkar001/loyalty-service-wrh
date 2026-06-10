package com.loyaltyos.coupon.enums;

public enum CouponValidationReason {
    VALID,
    NOT_FOUND,
    INACTIVE,
    EXPIRED,
    NOT_YET_VALID,
    EXHAUSTED,
    CUSTOMER_NOT_ELIGIBLE,
    MIN_ORDER_NOT_MET,
    CHANNEL_NOT_ALLOWED,
    ALREADY_USED_BY_CUSTOMER,
    NON_STACKABLE_CONFLICT,
    INVALID_TYPE_CONFIG
}
