package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record DashboardPointsEconomics(
    BigDecimal issuedToday,
    BigDecimal redeemedToday,
    BigDecimal netToday,
    BigDecimal burnRatePct30d
) {}
