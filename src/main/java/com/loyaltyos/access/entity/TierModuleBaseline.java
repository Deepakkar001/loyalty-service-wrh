package com.loyaltyos.access.entity;

import com.loyaltyos.onboarding.enums.SubscriptionTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "tier_module_baseline")
@IdClass(TierModuleBaselineId.class)
public class TierModuleBaseline {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", length = 32)
    private SubscriptionTier subscriptionTier;

    @Id
    @Column(name = "module_key", length = 64)
    private String moduleKey;

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    public TierModuleBaseline() {}

    public TierModuleBaseline(SubscriptionTier subscriptionTier, String moduleKey, boolean locked) {
        this.subscriptionTier = subscriptionTier;
        this.moduleKey = moduleKey;
        this.locked = locked;
    }

    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
