package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record ReversalAdjustmentDailyRow(
    String period,
    long reversalCount,
    BigDecimal reversalPoints,
    long adjustmentCount,
    BigDecimal adjustmentNetPoints
) {}
