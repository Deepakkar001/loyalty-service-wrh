package com.loyaltyos.referrals.dto;

import java.math.BigDecimal;

public record ReferralPeriodMetrics(
    long totalReferrals,
    long signedUp,
    long rewarded,
    long pending,
    long fraudFlagged,
    long rejected,
    long withPurchase,
    BigDecimal totalRefereeSpend,
    BigDecimal totalRewardPoints,
    BigDecimal conversionRatePercent,
    BigDecimal signupRatePercent,
    BigDecimal purchaseRatePercent
) {}
