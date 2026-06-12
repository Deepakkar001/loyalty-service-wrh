package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record ReconciliationDailyRow(
    String period,
    BigDecimal accruals,
    BigDecimal redemptions,
    BigDecimal expirations,
    BigDecimal reversals,
    BigDecimal adjustments,
    BigDecimal netChange
) {}
