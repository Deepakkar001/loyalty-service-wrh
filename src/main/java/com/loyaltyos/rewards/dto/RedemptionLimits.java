package com.loyaltyos.rewards.dto;

import java.math.BigDecimal;

/**
 * Programme redemption economics resolved from canonical config or legacy feature flags.
 */
public record RedemptionLimits(
    BigDecimal minRedemptionPoints,
    BigDecimal maxRedemptionPctPerTxn
) {
    public static RedemptionLimits none() {
        return new RedemptionLimits(null, null);
    }
}
