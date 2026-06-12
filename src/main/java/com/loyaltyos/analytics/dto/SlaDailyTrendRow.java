package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record SlaDailyTrendRow(
    String period,
    long apiRequests,
    BigDecimal apiSuccessRatePct,
    Integer apiAvgLatencyMs,
    long issuanceAttempts,
    BigDecimal issuanceSuccessRatePct,
    Integer issuanceAvgLatencyMs,
    long eventProcessingAttempts,
    BigDecimal eventSuccessRatePct,
    Integer eventAvgLatencyMs
) {}
