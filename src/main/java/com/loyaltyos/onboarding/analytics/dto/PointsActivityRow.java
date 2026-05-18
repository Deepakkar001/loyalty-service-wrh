package com.loyaltyos.onboarding.analytics.dto;

import java.math.BigDecimal;

public record PointsActivityRow(
    String reportDate,
    String entryType,
    long transactionCount,
    BigDecimal totalPoints,
    long uniqueCustomers
) {}
