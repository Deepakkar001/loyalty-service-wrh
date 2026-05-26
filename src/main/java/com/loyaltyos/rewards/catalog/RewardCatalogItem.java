package com.loyaltyos.rewards.catalog;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resolved redeemable reward from programme {@code rewardCatalog.items[]}.
 */
public record RewardCatalogItem(
    String rewardUid,
    String name,
    String rewardType,
    String status,
    BigDecimal pointsCost,
    int displayOrder,
    String description,
    Map<String, Object> metadata
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
