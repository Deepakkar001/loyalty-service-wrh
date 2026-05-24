package com.loyaltyos.analytics.dto;

public record TierUpgradeCohortRow(
    String cohortMonth,
    long cohortSize,
    long reachedSilver,
    double silverPct,
    Double avgDaysToSilver,
    long reachedGold,
    double goldPct,
    Double avgDaysToGold
) {}
