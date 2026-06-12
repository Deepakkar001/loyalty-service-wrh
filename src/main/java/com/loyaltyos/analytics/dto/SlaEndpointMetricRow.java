package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record SlaEndpointMetricRow(
    String operationKey,
    String operationLabel,
    long requestCount,
    long successCount,
    BigDecimal successRatePct,
    Integer avgLatencyMs,
    Integer p99LatencyMs
) {}
