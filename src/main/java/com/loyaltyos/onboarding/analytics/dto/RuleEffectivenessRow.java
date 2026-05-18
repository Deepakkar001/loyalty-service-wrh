package com.loyaltyos.onboarding.analytics.dto;

import java.math.BigDecimal;

public record RuleEffectivenessRow(
    String cohort,
    long memberCount,
    BigDecimal totalPointsEarned,
    long transactionCount,
    BigDecimal avgPointsPerMember
) {}
