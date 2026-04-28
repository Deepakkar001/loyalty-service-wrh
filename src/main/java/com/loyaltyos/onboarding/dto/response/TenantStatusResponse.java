package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TenantStatusResponse {
    private String tenantId;
    private String companyName;
    private String slug;
    private String email;
    private OnboardingStatus onboardingStatus;
    private IdentityMode identityMode;
    private SubscriptionTier subscriptionTier;
    private DataResidencyRegion dataResidencyRegion;
    private Boolean emailVerified;
    private AgreementStatus latestAgreementStatus;
    private String rejectionReason;
    private Instant createdAt;
    private Instant activatedAt;

    // Profile fields for Step 1 pre-fill
    private String businessCategory;
    private String countryCode;
    private String websiteUrl;
    private String timezone;
    private String primaryContactName;
    private String primaryContactEmail;
    private String primaryContactPhone;
    private String primaryContactDesignation;

    // Extended profile fields
    private String legalBusinessName;
    private String businessRegistrationNo;
    private String subCategory;
    private String businessModel;
    private Integer numberOfLocations;
    private String headquartersAddress;
    private String founderNames;
    private Integer yearFounded;
    private String annualRevenueRange;
    private Integer customerBaseSize;
    private String paymentMethodsAccepted;
}

