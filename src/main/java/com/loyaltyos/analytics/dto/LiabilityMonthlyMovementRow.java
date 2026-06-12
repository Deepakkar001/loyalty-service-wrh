package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record LiabilityMonthlyMovementRow(
    String month,
    boolean partialMonth,
    BigDecimal openingPoints,
    BigDecimal openingMonetary,
    BigDecimal pointsIssued,
    BigDecimal pointsRedeemed,
    BigDecimal pointsExpired,
    BigDecimal pointsReversed,
    BigDecimal adjustmentsNet,
    BigDecimal netChangePoints,
    BigDecimal netChangeMonetary,
    BigDecimal closingPoints,
    BigDecimal closingMonetary
) {}
