package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record SlaComponentMetricRow(
    String componentKey,
    String componentLabel,
    long totalOperations,
    long successfulOperations,
    BigDecimal successRatePct,
    Integer avgLatencyMs,
    Integer p50LatencyMs,
    Integer p95LatencyMs,
    Integer p99LatencyMs,
    Integer maxLatencyMs,
    BigDecimal slaSuccessRateTargetPct,
    Integer slaLatencyTargetMs,
    String slaStatus
) {}
