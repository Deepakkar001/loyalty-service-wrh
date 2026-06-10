package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record ExpirePeriodSummary(
    BigDecimal totalPoints,
    long customersAffected,
    long transactionCount
) {}
