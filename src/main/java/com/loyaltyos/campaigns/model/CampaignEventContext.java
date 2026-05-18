package com.loyaltyos.campaigns.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Customer + event inputs for campaign eligibility (orchestration supplies these in Phase 3).
 */
public record CampaignEventContext(
    String customerId,
    String customerTierUid,
    String eventType,
    BigDecimal amount,
    String channel,
    String country
) {
    public CampaignEventContext {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(eventType, "eventType");
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
    }
}
