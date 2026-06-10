package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;

public record CouponTypeBreakdownRow(
    String couponType,
    long redemptions,
    BigDecimal discountTotal,
    BigDecimal pointsCredited
) {}
