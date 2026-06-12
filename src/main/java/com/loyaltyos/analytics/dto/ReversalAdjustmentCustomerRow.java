package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record ReversalAdjustmentCustomerRow(
    String customerId,
    long reversalCount,
    BigDecimal reversalPoints,
    long adjustmentCount,
    BigDecimal adjustmentNetPoints
) {}
