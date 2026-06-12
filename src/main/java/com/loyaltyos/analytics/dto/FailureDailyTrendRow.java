package com.loyaltyos.analytics.dto;

public record FailureDailyTrendRow(
    String period,
    long accrualFailures,
    long redemptionFailures
) {}
