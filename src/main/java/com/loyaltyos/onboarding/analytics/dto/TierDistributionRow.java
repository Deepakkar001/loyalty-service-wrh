package com.loyaltyos.onboarding.analytics.dto;

import java.math.BigDecimal;

public record TierDistributionRow(
    String tierName,
    int rankOrder,
    long memberCount,
    BigDecimal entryThreshold,
    BigDecimal pointsMultiplier
) {}
