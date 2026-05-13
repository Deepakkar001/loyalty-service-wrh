package com.loyaltyos.onboarding.event;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;

import java.time.Instant;
import java.util.List;

/**
 * Published to Kafka topic: platform.config.updates
 * Kafka key: tenantId
 */
public class TenantActivatedEvent {

    private final String schemaVersion = "1.0";

    private String tenantId;
    private String slug;
    private String companyName;
    private IdentityMode identityMode;
    private SubscriptionTier subscriptionTier;
    private DataResidencyRegion dataResidencyRegion;
    private String businessCategory;
    private int maxActiveRules;
    private List<String> enabledFeatures;
    private Instant activatedAt;
    private String activatedByAdminId;

    public TenantActivatedEvent() {}

    public TenantActivatedEvent(
        String tenantId,
        String slug,
        String companyName,
        IdentityMode identityMode,
        SubscriptionTier subscriptionTier,
        DataResidencyRegion dataResidencyRegion,
        String businessCategory,
        int maxActiveRules,
        List<String> enabledFeatures,
        Instant activatedAt,
        String activatedByAdminId
    ) {
        this.tenantId = tenantId;
        this.slug = slug;
        this.companyName = companyName;
        this.identityMode = identityMode;
        this.subscriptionTier = subscriptionTier;
        this.dataResidencyRegion = dataResidencyRegion;
        this.businessCategory = businessCategory;
        this.maxActiveRules = maxActiveRules;
        this.enabledFeatures = enabledFeatures;
        this.activatedAt = activatedAt;
        this.activatedByAdminId = activatedByAdminId;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String slug;
        private String companyName;
        private IdentityMode identityMode;
        private SubscriptionTier subscriptionTier;
        private DataResidencyRegion dataResidencyRegion;
        private String businessCategory;
        private int maxActiveRules;
        private List<String> enabledFeatures;
        private Instant activatedAt;
        private String activatedByAdminId;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder businessCategory(String businessCategory) { this.businessCategory = businessCategory; return this; }
        public Builder maxActiveRules(int maxActiveRules) { this.maxActiveRules = maxActiveRules; return this; }
        public Builder enabledFeatures(List<String> enabledFeatures) { this.enabledFeatures = enabledFeatures; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder activatedByAdminId(String activatedByAdminId) { this.activatedByAdminId = activatedByAdminId; return this; }

        public TenantActivatedEvent build() {
            return new TenantActivatedEvent(
                tenantId,
                slug,
                companyName,
                identityMode,
                subscriptionTier,
                dataResidencyRegion,
                businessCategory,
                maxActiveRules,
                enabledFeatures,
                activatedAt,
                activatedByAdminId
            );
        }
    }

    public String getSchemaVersion() { return schemaVersion; }
    public String getTenantId() { return tenantId; }
    public String getSlug() { return slug; }
    public String getCompanyName() { return companyName; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public String getBusinessCategory() { return businessCategory; }
    public int getMaxActiveRules() { return maxActiveRules; }
    public List<String> getEnabledFeatures() { return enabledFeatures; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getActivatedByAdminId() { return activatedByAdminId; }
}

