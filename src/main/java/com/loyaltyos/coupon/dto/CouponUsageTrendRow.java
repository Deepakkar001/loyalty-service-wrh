package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;

public record CouponUsageTrendRow(
    String period,
    long redemptions,
    BigDecimal discountTotal,
    BigDecimal orderValueTotal,
    BigDecimal pointsCredited
) {}
