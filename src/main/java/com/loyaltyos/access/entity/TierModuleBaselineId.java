package com.loyaltyos.access.entity;

import com.loyaltyos.onboarding.enums.SubscriptionTier;

import java.io.Serializable;
import java.util.Objects;

public class TierModuleBaselineId implements Serializable {

    private SubscriptionTier subscriptionTier;
    private String moduleKey;

    public TierModuleBaselineId() {}

    public TierModuleBaselineId(SubscriptionTier subscriptionTier, String moduleKey) {
        this.subscriptionTier = subscriptionTier;
        this.moduleKey = moduleKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TierModuleBaselineId that)) return false;
        return subscriptionTier == that.subscriptionTier && Objects.equals(moduleKey, that.moduleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionTier, moduleKey);
    }
}
