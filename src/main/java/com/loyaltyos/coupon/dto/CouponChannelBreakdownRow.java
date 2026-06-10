package com.loyaltyos.coupon.dto;

import java.math.BigDecimal;

public record CouponChannelBreakdownRow(
    String channel,
    long redemptions,
    BigDecimal discountTotal,
    BigDecimal orderValueTotal
) {}
