package com.loyaltyos.rewards.catalog;

import java.util.List;

public record RewardCatalogSnapshot(
    int version,
    List<RewardCatalogTypeDefinition> rewardTypes,
    List<RewardCatalogItem> items
) {
    public static RewardCatalogSnapshot empty() {
        return new RewardCatalogSnapshot(1, List.of(), List.of());
    }
}
