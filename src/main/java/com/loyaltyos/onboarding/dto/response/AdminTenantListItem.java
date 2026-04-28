package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
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
}
