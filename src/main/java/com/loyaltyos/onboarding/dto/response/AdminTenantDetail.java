package com.loyaltyos.onboarding.dto.response;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;

import java.time.Instant;
import java.util.List;

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

    public static class TenantContactItem {
        private String contactName;
        private String contactEmail;
        private String contactPhone;
        private String designation;
        private String role;

        public TenantContactItem() {}

        public TenantContactItem(String contactName, String contactEmail, String contactPhone, String designation, String role) {
            this.contactName = contactName;
            this.contactEmail = contactEmail;
            this.contactPhone = contactPhone;
            this.designation = designation;
            this.role = role;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String contactName;
            private String contactEmail;
            private String contactPhone;
            private String designation;
            private String role;

            private Builder() {}

            public Builder contactName(String contactName) { this.contactName = contactName; return this; }
            public Builder contactEmail(String contactEmail) { this.contactEmail = contactEmail; return this; }
            public Builder contactPhone(String contactPhone) { this.contactPhone = contactPhone; return this; }
            public Builder designation(String designation) { this.designation = designation; return this; }
            public Builder role(String role) { this.role = role; return this; }

            public TenantContactItem build() {
                return new TenantContactItem(contactName, contactEmail, contactPhone, designation, role);
            }
        }

        public String getContactName() { return contactName; }
        public String getContactEmail() { return contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public String getDesignation() { return designation; }
        public String getRole() { return role; }
    }

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
        /** Optional notes entered by an admin when approving this agreement. */
        private String approvalNotes;
        private Instant createdAt;

        public AgreementHistoryItem() {}

        public AgreementHistoryItem(
            String agreementUid,
            String termsVersion,
            String effectiveDate,
            double revenueSharePct,
            String settlementFrequency,
            String signedByName,
            String signedByEmail,
            String signedByDesignation,
            Instant signedAt,
            String status,
            String approvedByAdminId,
            Instant approvedAt,
            String rejectionReason,
            String approvalNotes,
            Instant createdAt
        ) {
            this.agreementUid = agreementUid;
            this.termsVersion = termsVersion;
            this.effectiveDate = effectiveDate;
            this.revenueSharePct = revenueSharePct;
            this.settlementFrequency = settlementFrequency;
            this.signedByName = signedByName;
            this.signedByEmail = signedByEmail;
            this.signedByDesignation = signedByDesignation;
            this.signedAt = signedAt;
            this.status = status;
            this.approvedByAdminId = approvedByAdminId;
            this.approvedAt = approvedAt;
            this.rejectionReason = rejectionReason;
            this.approvalNotes = approvalNotes;
            this.createdAt = createdAt;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
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
            private String approvalNotes;
            private Instant createdAt;

            private Builder() {}

            public Builder agreementUid(String agreementUid) { this.agreementUid = agreementUid; return this; }
            public Builder termsVersion(String termsVersion) { this.termsVersion = termsVersion; return this; }
            public Builder effectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; return this; }
            public Builder revenueSharePct(double revenueSharePct) { this.revenueSharePct = revenueSharePct; return this; }
            public Builder settlementFrequency(String settlementFrequency) { this.settlementFrequency = settlementFrequency; return this; }
            public Builder signedByName(String signedByName) { this.signedByName = signedByName; return this; }
            public Builder signedByEmail(String signedByEmail) { this.signedByEmail = signedByEmail; return this; }
            public Builder signedByDesignation(String signedByDesignation) { this.signedByDesignation = signedByDesignation; return this; }
            public Builder signedAt(Instant signedAt) { this.signedAt = signedAt; return this; }
            public Builder status(String status) { this.status = status; return this; }
            public Builder approvedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; return this; }
            public Builder approvedAt(Instant approvedAt) { this.approvedAt = approvedAt; return this; }
            public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
            public Builder approvalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; return this; }
            public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

            public AgreementHistoryItem build() {
                return new AgreementHistoryItem(
                    agreementUid,
                    termsVersion,
                    effectiveDate,
                    revenueSharePct,
                    settlementFrequency,
                    signedByName,
                    signedByEmail,
                    signedByDesignation,
                    signedAt,
                    status,
                    approvedByAdminId,
                    approvedAt,
                    rejectionReason,
                    approvalNotes,
                    createdAt
                );
            }
        }

        public String getAgreementUid() { return agreementUid; }
        public String getTermsVersion() { return termsVersion; }
        public String getEffectiveDate() { return effectiveDate; }
        public double getRevenueSharePct() { return revenueSharePct; }
        public String getSettlementFrequency() { return settlementFrequency; }
        public String getSignedByName() { return signedByName; }
        public String getSignedByEmail() { return signedByEmail; }
        public String getSignedByDesignation() { return signedByDesignation; }
        public Instant getSignedAt() { return signedAt; }
        public String getStatus() { return status; }
        public String getApprovedByAdminId() { return approvedByAdminId; }
        public Instant getApprovedAt() { return approvedAt; }
        public String getRejectionReason() { return rejectionReason; }
        public String getApprovalNotes() { return approvalNotes; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public AdminTenantDetail() {}

    public AdminTenantDetail(
        String tenantId,
        String companyName,
        String slug,
        String email,
        String websiteUrl,
        String timezone,
        String countryCode,
        String businessCategory,
        OnboardingStatus onboardingStatus,
        IdentityMode identityMode,
        DataResidencyRegion dataResidencyRegion,
        SubscriptionTier subscriptionTier,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant terminatedAt,
        List<TenantContactItem> contacts,
        List<AgreementHistoryItem> agreements
    ) {
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.slug = slug;
        this.email = email;
        this.websiteUrl = websiteUrl;
        this.timezone = timezone;
        this.countryCode = countryCode;
        this.businessCategory = businessCategory;
        this.onboardingStatus = onboardingStatus;
        this.identityMode = identityMode;
        this.dataResidencyRegion = dataResidencyRegion;
        this.subscriptionTier = subscriptionTier;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activatedAt = activatedAt;
        this.suspendedAt = suspendedAt;
        this.terminatedAt = terminatedAt;
        this.contacts = contacts;
        this.agreements = agreements;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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

        private Builder() {}

        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder websiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder businessCategory(String businessCategory) { this.businessCategory = businessCategory; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder emailVerified(boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder suspendedAt(Instant suspendedAt) { this.suspendedAt = suspendedAt; return this; }
        public Builder terminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; return this; }
        public Builder contacts(List<TenantContactItem> contacts) { this.contacts = contacts; return this; }
        public Builder agreements(List<AgreementHistoryItem> agreements) { this.agreements = agreements; return this; }

        public AdminTenantDetail build() {
            return new AdminTenantDetail(
                tenantId,
                companyName,
                slug,
                email,
                websiteUrl,
                timezone,
                countryCode,
                businessCategory,
                onboardingStatus,
                identityMode,
                dataResidencyRegion,
                subscriptionTier,
                emailVerified,
                createdAt,
                updatedAt,
                activatedAt,
                suspendedAt,
                terminatedAt,
                contacts,
                agreements
            );
        }
    }

    public String getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getSlug() { return slug; }
    public String getEmail() { return email; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getTimezone() { return timezone; }
    public String getCountryCode() { return countryCode; }
    public String getBusinessCategory() { return businessCategory; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public Instant getTerminatedAt() { return terminatedAt; }
    public List<TenantContactItem> getContacts() { return contacts; }
    public List<AgreementHistoryItem> getAgreements() { return agreements; }
}
