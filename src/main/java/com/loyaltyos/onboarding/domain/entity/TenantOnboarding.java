package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_onboarding")
public class TenantOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    // External-facing unique identifier (UUID string) — used in all API paths
    @Column(name = "tenant_id", nullable = false, unique = true, length = 64)
    private String tenantId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "legal_business_name", length = 255)
    private String legalBusinessName;

    @Column(name = "business_registration_no", length = 100)
    private String businessRegistrationNo;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "business_category", nullable = false, length = 64)
    private String businessCategory;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_model", length = 30)
    private BusinessModel businessModel;

    @Column(name = "number_of_locations")
    private Integer numberOfLocations;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING_EMAIL_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_mode", nullable = false)
    private IdentityMode identityMode = IdentityMode.FULL_PROFILE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    private SubscriptionTier subscriptionTier = SubscriptionTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_residency_region", nullable = false)
    private DataResidencyRegion dataResidencyRegion = DataResidencyRegion.IN;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String countryCode;

    @Column(name = "headquarters_address", columnDefinition = "TEXT")
    private String headquartersAddress;

    @Column(name = "founder_names", length = 500)
    private String founderNames;

    @Column(name = "year_founded")
    private Integer yearFounded;

    @Enumerated(EnumType.STRING)
    @Column(name = "annual_revenue_range", length = 20)
    private AnnualRevenueRange annualRevenueRange;

    @Column(name = "customer_base_size")
    private Integer customerBaseSize;

    @Column(name = "payment_methods_accepted", length = 500)
    private String paymentMethodsAccepted;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone = "UTC";

    // Email verification
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expiry")
    private Instant emailVerificationExpiry;

    // Email verification (code-based)
    @Column(name = "email_verification_code_hash", length = 255)
    private String emailVerificationCodeHash;

    @Column(name = "email_verification_code_expiry")
    private Instant emailVerificationCodeExpiry;

    @Column(name = "email_verification_code_attempts", nullable = false)
    private Integer emailVerificationCodeAttempts = 0;

    @Column(name = "email_verification_code_last_sent_at")
    private Instant emailVerificationCodeLastSentAt;

    // Identity mode lock — set to true after first event is ingested by ingestion-service
    @Column(name = "is_identity_mode_locked", nullable = false)
    private Boolean identityModeLocked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "created_by_admin_id", length = 128)
    private String createdByAdminId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    /** JPA requires a no-arg constructor. */
    public TenantOnboarding() {}

    public TenantOnboarding(
        Long id,
        String tenantId,
        String companyName,
        String legalBusinessName,
        String businessRegistrationNo,
        String slug,
        String email,
        String passwordHash,
        String businessCategory,
        String subCategory,
        BusinessModel businessModel,
        Integer numberOfLocations,
        OnboardingStatus onboardingStatus,
        IdentityMode identityMode,
        SubscriptionTier subscriptionTier,
        DataResidencyRegion dataResidencyRegion,
        String websiteUrl,
        String countryCode,
        String headquartersAddress,
        String founderNames,
        Integer yearFounded,
        AnnualRevenueRange annualRevenueRange,
        Integer customerBaseSize,
        String paymentMethodsAccepted,
        String timezone,
        Boolean emailVerified,
        String emailVerificationToken,
        Instant emailVerificationExpiry,
        String emailVerificationCodeHash,
        Instant emailVerificationCodeExpiry,
        Integer emailVerificationCodeAttempts,
        Instant emailVerificationCodeLastSentAt,
        Boolean identityModeLocked,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant terminatedAt,
        String createdByAdminId,
        Integer version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.legalBusinessName = legalBusinessName;
        this.businessRegistrationNo = businessRegistrationNo;
        this.slug = slug;
        this.email = email;
        this.passwordHash = passwordHash;
        this.businessCategory = businessCategory;
        this.subCategory = subCategory;
        this.businessModel = businessModel;
        this.numberOfLocations = numberOfLocations;
        this.onboardingStatus = onboardingStatus != null ? onboardingStatus : OnboardingStatus.PENDING_EMAIL_VERIFICATION;
        this.identityMode = identityMode != null ? identityMode : IdentityMode.FULL_PROFILE;
        this.subscriptionTier = subscriptionTier != null ? subscriptionTier : SubscriptionTier.STANDARD;
        this.dataResidencyRegion = dataResidencyRegion != null ? dataResidencyRegion : DataResidencyRegion.IN;
        this.websiteUrl = websiteUrl;
        this.countryCode = countryCode;
        this.headquartersAddress = headquartersAddress;
        this.founderNames = founderNames;
        this.yearFounded = yearFounded;
        this.annualRevenueRange = annualRevenueRange;
        this.customerBaseSize = customerBaseSize;
        this.paymentMethodsAccepted = paymentMethodsAccepted;
        this.timezone = timezone != null ? timezone : "UTC";
        this.emailVerified = emailVerified != null ? emailVerified : false;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerificationExpiry = emailVerificationExpiry;
        this.emailVerificationCodeHash = emailVerificationCodeHash;
        this.emailVerificationCodeExpiry = emailVerificationCodeExpiry;
        this.emailVerificationCodeAttempts = emailVerificationCodeAttempts != null ? emailVerificationCodeAttempts : 0;
        this.emailVerificationCodeLastSentAt = emailVerificationCodeLastSentAt;
        this.identityModeLocked = identityModeLocked != null ? identityModeLocked : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activatedAt = activatedAt;
        this.suspendedAt = suspendedAt;
        this.terminatedAt = terminatedAt;
        this.createdByAdminId = createdByAdminId;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private String tenantId;
        private String companyName;
        private String legalBusinessName;
        private String businessRegistrationNo;
        private String slug;
        private String email;
        private String passwordHash;
        private String businessCategory;
        private String subCategory;
        private BusinessModel businessModel;
        private Integer numberOfLocations;
        private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING_EMAIL_VERIFICATION;
        private IdentityMode identityMode = IdentityMode.FULL_PROFILE;
        private SubscriptionTier subscriptionTier = SubscriptionTier.STANDARD;
        private DataResidencyRegion dataResidencyRegion = DataResidencyRegion.IN;
        private String websiteUrl;
        private String countryCode;
        private String headquartersAddress;
        private String founderNames;
        private Integer yearFounded;
        private AnnualRevenueRange annualRevenueRange;
        private Integer customerBaseSize;
        private String paymentMethodsAccepted;
        private String timezone = "UTC";
        private Boolean emailVerified = false;
        private String emailVerificationToken;
        private Instant emailVerificationExpiry;
        private String emailVerificationCodeHash;
        private Instant emailVerificationCodeExpiry;
        private Integer emailVerificationCodeAttempts = 0;
        private Instant emailVerificationCodeLastSentAt;
        private Boolean identityModeLocked = false;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant activatedAt;
        private Instant suspendedAt;
        private Instant terminatedAt;
        private String createdByAdminId;
        private Integer version;

        private Builder() {}

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder legalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; return this; }
        public Builder businessRegistrationNo(String businessRegistrationNo) { this.businessRegistrationNo = businessRegistrationNo; return this; }
        public Builder slug(String slug) { this.slug = slug; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder businessCategory(String businessCategory) { this.businessCategory = businessCategory; return this; }
        public Builder subCategory(String subCategory) { this.subCategory = subCategory; return this; }
        public Builder businessModel(BusinessModel businessModel) { this.businessModel = businessModel; return this; }
        public Builder numberOfLocations(Integer numberOfLocations) { this.numberOfLocations = numberOfLocations; return this; }
        public Builder onboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; return this; }
        public Builder identityMode(IdentityMode identityMode) { this.identityMode = identityMode; return this; }
        public Builder subscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; return this; }
        public Builder dataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; return this; }
        public Builder websiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder headquartersAddress(String headquartersAddress) { this.headquartersAddress = headquartersAddress; return this; }
        public Builder founderNames(String founderNames) { this.founderNames = founderNames; return this; }
        public Builder yearFounded(Integer yearFounded) { this.yearFounded = yearFounded; return this; }
        public Builder annualRevenueRange(AnnualRevenueRange annualRevenueRange) { this.annualRevenueRange = annualRevenueRange; return this; }
        public Builder customerBaseSize(Integer customerBaseSize) { this.customerBaseSize = customerBaseSize; return this; }
        public Builder paymentMethodsAccepted(String paymentMethodsAccepted) { this.paymentMethodsAccepted = paymentMethodsAccepted; return this; }
        public Builder timezone(String timezone) { this.timezone = timezone; return this; }
        public Builder emailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public Builder emailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; return this; }
        public Builder emailVerificationExpiry(Instant emailVerificationExpiry) { this.emailVerificationExpiry = emailVerificationExpiry; return this; }
        public Builder emailVerificationCodeHash(String emailVerificationCodeHash) { this.emailVerificationCodeHash = emailVerificationCodeHash; return this; }
        public Builder emailVerificationCodeExpiry(Instant emailVerificationCodeExpiry) { this.emailVerificationCodeExpiry = emailVerificationCodeExpiry; return this; }
        public Builder emailVerificationCodeAttempts(Integer emailVerificationCodeAttempts) { this.emailVerificationCodeAttempts = emailVerificationCodeAttempts; return this; }
        public Builder emailVerificationCodeLastSentAt(Instant emailVerificationCodeLastSentAt) { this.emailVerificationCodeLastSentAt = emailVerificationCodeLastSentAt; return this; }
        public Builder identityModeLocked(Boolean identityModeLocked) { this.identityModeLocked = identityModeLocked; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder activatedAt(Instant activatedAt) { this.activatedAt = activatedAt; return this; }
        public Builder suspendedAt(Instant suspendedAt) { this.suspendedAt = suspendedAt; return this; }
        public Builder terminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; return this; }
        public Builder createdByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; return this; }
        public Builder version(Integer version) { this.version = version; return this; }

        public TenantOnboarding build() {
            return new TenantOnboarding(
                id,
                tenantId,
                companyName,
                legalBusinessName,
                businessRegistrationNo,
                slug,
                email,
                passwordHash,
                businessCategory,
                subCategory,
                businessModel,
                numberOfLocations,
                onboardingStatus,
                identityMode,
                subscriptionTier,
                dataResidencyRegion,
                websiteUrl,
                countryCode,
                headquartersAddress,
                founderNames,
                yearFounded,
                annualRevenueRange,
                customerBaseSize,
                paymentMethodsAccepted,
                timezone,
                emailVerified,
                emailVerificationToken,
                emailVerificationExpiry,
                emailVerificationCodeHash,
                emailVerificationCodeExpiry,
                emailVerificationCodeAttempts,
                emailVerificationCodeLastSentAt,
                identityModeLocked,
                createdAt,
                updatedAt,
                activatedAt,
                suspendedAt,
                terminatedAt,
                createdByAdminId,
                version
            );
        }
    }

    // Getters/setters (explicit; avoids Lombok)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLegalBusinessName() { return legalBusinessName; }
    public void setLegalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; }
    public String getBusinessRegistrationNo() { return businessRegistrationNo; }
    public void setBusinessRegistrationNo(String businessRegistrationNo) { this.businessRegistrationNo = businessRegistrationNo; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public BusinessModel getBusinessModel() { return businessModel; }
    public void setBusinessModel(BusinessModel businessModel) { this.businessModel = businessModel; }
    public Integer getNumberOfLocations() { return numberOfLocations; }
    public void setNumberOfLocations(Integer numberOfLocations) { this.numberOfLocations = numberOfLocations; }
    public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public void setOnboardingStatus(OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; }
    public IdentityMode getIdentityMode() { return identityMode; }
    public void setIdentityMode(IdentityMode identityMode) { this.identityMode = identityMode; }
    public SubscriptionTier getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(SubscriptionTier subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getHeadquartersAddress() { return headquartersAddress; }
    public void setHeadquartersAddress(String headquartersAddress) { this.headquartersAddress = headquartersAddress; }
    public String getFounderNames() { return founderNames; }
    public void setFounderNames(String founderNames) { this.founderNames = founderNames; }
    public Integer getYearFounded() { return yearFounded; }
    public void setYearFounded(Integer yearFounded) { this.yearFounded = yearFounded; }
    public AnnualRevenueRange getAnnualRevenueRange() { return annualRevenueRange; }
    public void setAnnualRevenueRange(AnnualRevenueRange annualRevenueRange) { this.annualRevenueRange = annualRevenueRange; }
    public Integer getCustomerBaseSize() { return customerBaseSize; }
    public void setCustomerBaseSize(Integer customerBaseSize) { this.customerBaseSize = customerBaseSize; }
    public String getPaymentMethodsAccepted() { return paymentMethodsAccepted; }
    public void setPaymentMethodsAccepted(String paymentMethodsAccepted) { this.paymentMethodsAccepted = paymentMethodsAccepted; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }
    public Instant getEmailVerificationExpiry() { return emailVerificationExpiry; }
    public void setEmailVerificationExpiry(Instant emailVerificationExpiry) { this.emailVerificationExpiry = emailVerificationExpiry; }
    public String getEmailVerificationCodeHash() { return emailVerificationCodeHash; }
    public void setEmailVerificationCodeHash(String emailVerificationCodeHash) { this.emailVerificationCodeHash = emailVerificationCodeHash; }
    public Instant getEmailVerificationCodeExpiry() { return emailVerificationCodeExpiry; }
    public void setEmailVerificationCodeExpiry(Instant emailVerificationCodeExpiry) { this.emailVerificationCodeExpiry = emailVerificationCodeExpiry; }
    public Integer getEmailVerificationCodeAttempts() { return emailVerificationCodeAttempts; }
    public void setEmailVerificationCodeAttempts(Integer emailVerificationCodeAttempts) { this.emailVerificationCodeAttempts = emailVerificationCodeAttempts; }
    public Instant getEmailVerificationCodeLastSentAt() { return emailVerificationCodeLastSentAt; }
    public void setEmailVerificationCodeLastSentAt(Instant emailVerificationCodeLastSentAt) { this.emailVerificationCodeLastSentAt = emailVerificationCodeLastSentAt; }
    public Boolean getIdentityModeLocked() { return identityModeLocked; }
    public void setIdentityModeLocked(Boolean identityModeLocked) { this.identityModeLocked = identityModeLocked; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Instant suspendedAt) { this.suspendedAt = suspendedAt; }
    public Instant getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public String toString() {
        return "TenantOnboarding{" +
            "id=" + id +
            ", tenantId='" + tenantId + '\'' +
            ", companyName='" + companyName + '\'' +
            ", legalBusinessName='" + legalBusinessName + '\'' +
            ", businessRegistrationNo='" + businessRegistrationNo + '\'' +
            ", slug='" + slug + '\'' +
            ", email='" + email + '\'' +
            ", passwordHash=[PROTECTED]" +
            '}';
    }
}

