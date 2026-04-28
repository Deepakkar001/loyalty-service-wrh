package com.loyaltyos.onboarding.domain.entity;

import com.loyaltyos.onboarding.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenant_onboarding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "passwordHash")
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
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING_EMAIL_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_mode", nullable = false)
    @Builder.Default
    private IdentityMode identityMode = IdentityMode.FULL_PROFILE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    @Builder.Default
    private SubscriptionTier subscriptionTier = SubscriptionTier.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_residency_region", nullable = false)
    @Builder.Default
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
    @Builder.Default
    private String timezone = "UTC";

    // Email verification
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
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
    @Builder.Default
    private Integer emailVerificationCodeAttempts = 0;

    @Column(name = "email_verification_code_last_sent_at")
    private Instant emailVerificationCodeLastSentAt;

    // Identity mode lock — set to true after first event is ingested by ingestion-service
    @Column(name = "is_identity_mode_locked", nullable = false)
    @Builder.Default
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
}

