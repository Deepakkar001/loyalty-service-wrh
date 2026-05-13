package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;

import java.time.Instant;

public class AdminTenantListItem {
    private String tenantId;
    private String companyName;
    private String slug;
    private String email;
    private String businessCategory;
    private String countryCode;
    private OnboardingStatus onboardingStatus;
    private IdentityMode identityMode;
    private DataResidencyRegion dataResidencyRegion;
    private SubscriptionTier subscriptionTier;
    private boolean emailVerified;
    private String latestAgreementStatus;
    private Instant createdAt;
    private Instant activatedAt;

    public AdminTenantListItem() {}

    public AdminTenantListItem(
        String tenantId,
        String companyName,
        String slug,
        String email,
        String businessCategory,
        String countryCode,
        OnboardingStatus onboardingStatus,
        IdentityMode identityMode,
        DataResidencyRegion dataResidencyRegion,
        SubscriptionTier subscriptionTier,
        boolean emailVerified,
        String latestAgreementStatus,
        Instant createdAt,
        Instant activatedAt
    ) {
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.slug = slug;
        this.email = email;
        this.businessCategory = businessCategory;
        this.countryCode = countryCode;
        this.onboardingStatus = onboardingStatus;
        this.identityMode = identityMode;
        this.dataResidencyRegion = dataResidencyRegion;
        this.subscriptionTier = subscriptionTier;
        this.emailVerified = emailVerified;
        this.latestAgreementStatus = latestAgreementStatus;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tenantId;
        private String companyName;
        private String slug;
        private String email;
        private String businessCategory;
        private String countryCode;
        private OnboardingStatus onboardingStatus;
        private IdentityMode identityMode;
        private DataResidencyRegion dataResidencyRegion;
        private SubscriptionTier subscriptionTier;
        private boolean emailVerified;
        private String latestAgreementStatus;
        private Instant createdAt;
        private Instant activatedAt;

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder businessCategory(String businessCategory) { this.businessCategory = businessCategory; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder emailVerified(boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public Builder latestAgreementStatus(String latestAgreementStatus) { this.latestAgreementStatus = latestAgreementStatus; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }

        public AdminTenantListItem build() {
            return new AdminTenantListItem(
                tenantId,
                companyName,
                slug,
                email,
                businessCategory,
                countryCode,
                onboardingStatus,
                identityMode,
                dataResidencyRegion,
                subscriptionTier,
                emailVerified,
                latestAgreementStatus,
                createdAt,
                activatedAt
            );
        }
    }

    public String getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getSlug() { return slug; }
    public String getEmail() { return email; }
    public String getBusinessCategory() { return businessCategory; }
    public String getCountryCode() { return countryCode; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getLatestAgreementStatus() { return latestAgreementStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
}
