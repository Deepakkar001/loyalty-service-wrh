package com.loyaltyos.onboarding.analytics.dto;

public record CohortRetentionRow(
    String cohortMonth,
    long cohortSize,
    int monthsSinceJoin,
    long activeCustomers,
    double retentionPct
) {}
