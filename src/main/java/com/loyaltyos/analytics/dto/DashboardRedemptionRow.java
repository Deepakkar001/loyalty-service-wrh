package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record DashboardRedemptionRow(
    String label,
    long redemptionCount,
    BigDecimal totalPoints
) {}
