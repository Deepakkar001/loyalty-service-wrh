package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;

public record LedgerMovementAggregate(
    String entryType,
    BigDecimal pointsMagnitude,
    BigDecimal signedPointsImpact,
    long transactionCount,
    long uniqueCustomers
) {}
