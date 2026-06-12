package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record LiabilityTierBreakdownRow(
    String tierName,
    int rankOrder,
    long memberCount,
    BigDecimal pointsLiability,
    BigDecimal monetaryLiability
) {}
