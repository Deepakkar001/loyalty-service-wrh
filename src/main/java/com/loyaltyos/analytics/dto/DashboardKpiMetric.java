package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record DashboardKpiMetric(
    BigDecimal value,
    Double trendPct
) {}
