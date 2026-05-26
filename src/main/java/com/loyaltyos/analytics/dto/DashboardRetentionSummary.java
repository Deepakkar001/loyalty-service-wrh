package com.loyaltyos.analytics.dto;

public record DashboardRetentionSummary(
    Double latestRetentionPct,
    String cohortMonth
) {}
