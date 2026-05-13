package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.AgreementStatus;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;

import java.time.Instant;

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
    /** Optional notes entered by an admin when approving the latest agreement. */
    private String approvalNotes;
    private Instant createdAt;
    private Instant activatedAt;

    // Profile fields for Step 1 pre-fill
    private String businessCategory;
    /** Human-readable label of the resolved business category (helpful when status != APPROVED). */
    private String businessCategoryLabel;
    /** Moderation status of the resolved category: APPROVED / PENDING_REVIEW / REJECTED. */
    private String businessCategoryStatus;
    /** Optional rejection reason when {@link #businessCategoryStatus} is REJECTED. */
    private String businessCategoryDecisionReason;
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

    public TenantStatusResponse() {}

    public TenantStatusResponse(
        String tenantId,
        String companyName,
        String slug,
        String email,
        OnboardingStatus onboardingStatus,
        IdentityMode identityMode,
        SubscriptionTier subscriptionTier,
        DataResidencyRegion dataResidencyRegion,
        Boolean emailVerified,
        AgreementStatus latestAgreementStatus,
        String rejectionReason,
        String approvalNotes,
        Instant createdAt,
        Instant activatedAt,
        String businessCategory,
        String businessCategoryLabel,
        String businessCategoryStatus,
        String businessCategoryDecisionReason,
        String countryCode,
        String websiteUrl,
        String timezone,
        String primaryContactName,
        String primaryContactEmail,
        String primaryContactPhone,
        String primaryContactDesignation,
        String legalBusinessName,
        String businessRegistrationNo,
        String subCategory,
        String businessModel,
        Integer numberOfLocations,
        String headquartersAddress,
        String founderNames,
        Integer yearFounded,
        String annualRevenueRange,
        Integer customerBaseSize,
        String paymentMethodsAccepted
    ) {
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.slug = slug;
        this.email = email;
        this.onboardingStatus = onboardingStatus;
        this.identityMode = identityMode;
        this.subscriptionTier = subscriptionTier;
        this.dataResidencyRegion = dataResidencyRegion;
        this.emailVerified = emailVerified;
        this.latestAgreementStatus = latestAgreementStatus;
        this.rejectionReason = rejectionReason;
        this.approvalNotes = approvalNotes;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.businessCategory = businessCategory;
        this.businessCategoryLabel = businessCategoryLabel;
        this.businessCategoryStatus = businessCategoryStatus;
        this.businessCategoryDecisionReason = businessCategoryDecisionReason;
        this.countryCode = countryCode;
        this.websiteUrl = websiteUrl;
        this.timezone = timezone;
        this.primaryContactName = primaryContactName;
        this.primaryContactEmail = primaryContactEmail;
        this.primaryContactPhone = primaryContactPhone;
        this.primaryContactDesignation = primaryContactDesignation;
        this.legalBusinessName = legalBusinessName;
        this.businessRegistrationNo = businessRegistrationNo;
        this.subCategory = subCategory;
        this.businessModel = businessModel;
        this.numberOfLocations = numberOfLocations;
        this.headquartersAddress = headquartersAddress;
        this.founderNames = founderNames;
        this.yearFounded = yearFounded;
        this.annualRevenueRange = annualRevenueRange;
        this.customerBaseSize = customerBaseSize;
        this.paymentMethodsAccepted = paymentMethodsAccepted;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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
        private String approvalNotes;
        private Instant createdAt;
        private Instant activatedAt;
        private String businessCategory;
        private String businessCategoryLabel;
        private String businessCategoryStatus;
        private String businessCategoryDecisionReason;
        private String countryCode;
        private String websiteUrl;
        private String timezone;
        private String primaryContactName;
        private String primaryContactEmail;
        private String primaryContactPhone;
        private String primaryContactDesignation;
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

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder emailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public Builder latestAgreementStatus(AgreementStatus latestAgreementStatus) { this.latestAgreementStatus = latestAgreementStatus; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder approvalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder businessCategory(String businessCategory) { this.businessCategory = businessCategory; return this; }
        public Builder businessCategoryLabel(String businessCategoryLabel) { this.businessCategoryLabel = businessCategoryLabel; return this; }
        public Builder businessCategoryStatus(String businessCategoryStatus) { this.businessCategoryStatus = businessCategoryStatus; return this; }
        public Builder businessCategoryDecisionReason(String businessCategoryDecisionReason) { this.businessCategoryDecisionReason = businessCategoryDecisionReason; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder websiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }
        public Builder primaryContactName(String primaryContactName) { this.primaryContactName = primaryContactName; return this; }
        public Builder primaryContactEmail(String primaryContactEmail) { this.primaryContactEmail = primaryContactEmail; return this; }
        public Builder primaryContactPhone(String primaryContactPhone) { this.primaryContactPhone = primaryContactPhone; return this; }
        public Builder primaryContactDesignation(String primaryContactDesignation) { this.primaryContactDesignation = primaryContactDesignation; return this; }
        public Builder legalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; return this; }
        public Builder businessRegistrationNo(String businessRegistrationNo) { this.businessRegistrationNo = businessRegistrationNo; return this; }
        public Builder subCategory(String subCategory) { this.subCategory = subCategory; return this; }
        public Builder businessModel(String businessModel) { this.businessModel = businessModel; return this; }
        public Builder numberOfLocations(Integer numberOfLocations) { this.numberOfLocations = numberOfLocations; return this; }
        public Builder headquartersAddress(String headquartersAddress) { this.headquartersAddress = headquartersAddress; return this; }
        public Builder founderNames(String founderNames) { this.founderNames = founderNames; return this; }
        public Builder yearFounded(Integer yearFounded) { this.yearFounded = yearFounded; return this; }
        public Builder annualRevenueRange(String annualRevenueRange) { this.annualRevenueRange = annualRevenueRange; return this; }
        public Builder customerBaseSize(Integer customerBaseSize) { this.customerBaseSize = customerBaseSize; return this; }
        public Builder paymentMethodsAccepted(String paymentMethodsAccepted) { this.paymentMethodsAccepted = paymentMethodsAccepted; return this; }

        public TenantStatusResponse build() {
            return new TenantStatusResponse(
                tenantId,
                companyName,
                slug,
                email,
                onboardingStatus,
                identityMode,
                subscriptionTier,
                dataResidencyRegion,
                emailVerified,
                latestAgreementStatus,
                rejectionReason,
                approvalNotes,
                createdAt,
                activatedAt,
                businessCategory,
                businessCategoryLabel,
                businessCategoryStatus,
                businessCategoryDecisionReason,
                countryCode,
                websiteUrl,
                timezone,
                primaryContactName,
                primaryContactEmail,
                primaryContactPhone,
                primaryContactDesignation,
                legalBusinessName,
                businessRegistrationNo,
                subCategory,
                businessModel,
                numberOfLocations,
                headquartersAddress,
                founderNames,
                yearFounded,
                annualRevenueRange,
                customerBaseSize,
                paymentMethodsAccepted
            );
        }
    }

    public String getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getSlug() { return slug; }
    public String getEmail() { return email; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public Boolean getEmailVerified() { return emailVerified; }
    public AgreementStatus getLatestAgreementStatus() { return latestAgreementStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public String getApprovalNotes() { return approvalNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getBusinessCategory() { return businessCategory; }
    public String getBusinessCategoryLabel() { return businessCategoryLabel; }
    public String getBusinessCategoryStatus() { return businessCategoryStatus; }
    public String getBusinessCategoryDecisionReason() { return businessCategoryDecisionReason; }
    public String getCountryCode() { return countryCode; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getTimezone() { return timezone; }
    public String getPrimaryContactName() { return primaryContactName; }
    public String getPrimaryContactEmail() { return primaryContactEmail; }
    public String getPrimaryContactPhone() { return primaryContactPhone; }
    public String getPrimaryContactDesignation() { return primaryContactDesignation; }
    public String getLegalBusinessName() { return legalBusinessName; }
    public String getBusinessRegistrationNo() { return businessRegistrationNo; }
    public String getSubCategory() { return subCategory; }
    public String getBusinessModel() { return businessModel; }
    public Integer getNumberOfLocations() { return numberOfLocations; }
    public String getHeadquartersAddress() { return headquartersAddress; }
    public String getFounderNames() { return founderNames; }
    public Integer getYearFounded() { return yearFounded; }
    public String getAnnualRevenueRange() { return annualRevenueRange; }
    public Integer getCustomerBaseSize() { return customerBaseSize; }
    public String getPaymentMethodsAccepted() { return paymentMethodsAccepted; }
}

