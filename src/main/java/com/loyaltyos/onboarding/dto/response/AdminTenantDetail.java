package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AdminTenantDetail {
    private String tenantId;
    private String companyName;
    private String slug;
    private String email;
    private String websiteUrl;
    private String timezone;
    private String countryCode;
    private String businessCategory;
    private OnboardingStatus onboardingStatus;
    private IdentityMode identityMode;
    private DataResidencyRegion dataResidencyRegion;
    private SubscriptionTier subscriptionTier;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant activatedAt;
    private Instant suspendedAt;
    private Instant terminatedAt;

    private List<TenantContactItem> contacts;
    private List<AgreementHistoryItem> agreements;

    @Getter
    @Builder
    public static class TenantContactItem {
        private String contactName;
        private String contactEmail;
        private String contactPhone;
        private String designation;
        private String role;
    }

    @Getter
    @Builder
    public static class AgreementHistoryItem {
        private String agreementUid;
        private String termsVersion;
        private String effectiveDate;
        private double revenueSharePct;
        private String settlementFrequency;
        private String signedByName;
        private String signedByEmail;
        private String signedByDesignation;
        private Instant signedAt;
        private String status;
        private String approvedByAdminId;
        private Instant approvedAt;
        private String rejectionReason;
        private Instant createdAt;
    }
}
