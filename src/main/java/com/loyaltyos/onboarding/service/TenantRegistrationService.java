package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.OnboardingAuditLog;
import com.loyaltyos.onboarding.domain.entity.RefBusinessCategory;
import com.loyaltyos.onboarding.domain.entity.TenantContact;
import com.loyaltyos.onboarding.domain.entity.TenantOnboarding;
import com.loyaltyos.onboarding.domain.enums.AnnualRevenueRange;
import com.loyaltyos.onboarding.domain.enums.BusinessCategoryStatus;
import com.loyaltyos.onboarding.domain.enums.BusinessModel;
import com.loyaltyos.onboarding.domain.enums.ContactRole;
import com.loyaltyos.onboarding.domain.enums.OnboardingStatus;
import com.loyaltyos.onboarding.domain.enums.SubscriptionTier;
import com.loyaltyos.onboarding.dto.request.RegisterTenantRequest;
import com.loyaltyos.onboarding.dto.request.ResendVerificationRequest;
import com.loyaltyos.onboarding.dto.request.UpdateIdentityRequest;
import com.loyaltyos.onboarding.dto.request.UpdateProfileRequest;
import com.loyaltyos.onboarding.dto.response.TenantRegistrationResponse;
import com.loyaltyos.onboarding.dto.response.TenantStatusResponse;
import com.loyaltyos.onboarding.exception.DuplicateTenantException;
import com.loyaltyos.onboarding.exception.InvalidVerificationCodeException;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.onboarding.exception.VerificationRateLimitException;
import com.loyaltyos.onboarding.repository.OnboardingAuditLogRepository;
import com.loyaltyos.onboarding.repository.RefBusinessCategoryRepository;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantContactRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.service.statemachine.OnboardingStateMachine;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class TenantRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(TenantRegistrationService.class);

    private final TenantOnboardingRepository tenantRepository;
    private final TenantContactRepository contactRepository;
    private final TenantAgreementRepository agreementRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final RefBusinessCategoryRepository businessCategoryRepository;
    private final OnboardingStateMachine stateMachine;
    private final PasswordEncoder passwordEncoder;
    private final SlugGenerationService slugGenerationService;
    private final EmailVerificationMailer emailVerificationMailer;
    private final StringRedisTemplate redis;
    private final OnboardingDeletionService deletionService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_CODE_TTL_MINUTES = 10;
    private static final int VERIFICATION_CODE_MAX_ATTEMPTS = 5;
    private static final int VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS = 30;
    private static final int OTP_MAX_SENDS_PER_24H = 10;
    private static final Duration OTP_SEND_WINDOW = Duration.ofHours(24);
    private static final String OTP_SEND_PREFIX = "otp:send:";
    private static final int OTP_MAX_SENDS_PER_IP_24H = 50;
    private static final String OTP_SEND_IP_PREFIX = "otp:ip:";

    @org.springframework.beans.factory.annotation.Value("${app.onboarding.pending-expiry-hours:24}")
    private long pendingExpiryHours;

    public TenantRegistrationService(
        TenantOnboardingRepository tenantRepository,
        TenantContactRepository contactRepository,
        TenantAgreementRepository agreementRepository,
        OnboardingAuditLogRepository auditLogRepository,
        RefBusinessCategoryRepository businessCategoryRepository,
        OnboardingStateMachine stateMachine,
        PasswordEncoder passwordEncoder,
        SlugGenerationService slugGenerationService,
        EmailVerificationMailer emailVerificationMailer,
        StringRedisTemplate redis,
        OnboardingDeletionService deletionService
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository");
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.businessCategoryRepository = Objects.requireNonNull(businessCategoryRepository, "businessCategoryRepository");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.slugGenerationService = Objects.requireNonNull(slugGenerationService, "slugGenerationService");
        this.emailVerificationMailer = Objects.requireNonNull(emailVerificationMailer, "emailVerificationMailer");
        this.redis = Objects.requireNonNull(redis, "redis");
        this.deletionService = Objects.requireNonNull(deletionService, "deletionService");
    }

    @Transactional
    public TenantRegistrationResponse register(RegisterTenantRequest request, String clientIp) {

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        var existing = tenantRepository.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            // Verified accounts remain unique and cannot be re-registered.
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                // Keep behavior (409) but avoid echoing PII in message.
                throw new DuplicateTenantException("email", "<redacted>");
            }

            // If this pending signup is stale, delete it and allow a fresh registration.
            Instant cutoff = Instant.now().minus(pendingExpiryHours, ChronoUnit.HOURS);
            if (existing.getCreatedAt() != null && existing.getCreatedAt().isBefore(cutoff)) {
                deletionService.deleteTenantOnboardingData(existing.getTenantId());
                existing = null;
            } else {
                // Resume unfinished registration: allow OTP to proceed even if password was mistyped.
                // Password can be set during OTP verification (newPassword field).

                // Respect resend cooldown; if still cooling down, just return the existing status.
                if (existing.getEmailVerificationCodeLastSentAt() != null) {
                    Instant nextAllowed = existing.getEmailVerificationCodeLastSentAt()
                        .plus(VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS);
                    if (Instant.now().isBefore(nextAllowed)) {
                        return TenantRegistrationResponse.builder()
                            .tenantId(existing.getTenantId())
                            .slug(existing.getSlug())
                            .email(existing.getEmail())
                            .onboardingStatus(existing.getOnboardingStatus())
                            .identityMode(existing.getIdentityMode())
                            .message("If this email can be registered, we sent a verification code.")
                            .createdAt(existing.getCreatedAt())
                            .build();
                    }
                }

                // Re-issue verification code for pending signup.
                String token = generateVerificationToken();
                String code = generateVerificationCode();
                existing.setEmailVerificationToken(token);
                existing.setEmailVerificationExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
                existing.setEmailVerificationCodeHash(passwordEncoder.encode(code));
                existing.setEmailVerificationCodeExpiry(Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES));
                existing.setEmailVerificationCodeAttempts(0);
                existing.setEmailVerificationCodeLastSentAt(Instant.now());

                tenantRepository.save(existing);

                OnboardingAuditLog audit = OnboardingAuditLog.builder()
                    .tenantId(existing.getTenantId())
                    .action("REGISTRATION_RESUMED_OTP_SENT")
                    .actorRole("SELF")
                    .actorId(existing.getTenantId())
                    .afterState(Map.of("email", normalizedEmail))
                    .build();
                auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

                enforceOtpLimits(normalizedEmail, clientIp);
                emailVerificationMailer.sendVerificationCodeEmail(normalizedEmail, code);

                return TenantRegistrationResponse.builder()
                    .tenantId(existing.getTenantId())
                    .slug(existing.getSlug())
                    .email(existing.getEmail())
                    .onboardingStatus(existing.getOnboardingStatus())
                    .identityMode(existing.getIdentityMode())
                    .message("If this email can be registered, we sent a verification code.")
                    .createdAt(existing.getCreatedAt())
                    .build();
            }
        }

        String tenantId = UUID.randomUUID().toString();
        String slug = slugGenerationService.generateUniqueSlug(request.getCompanyName());
        String verificationToken = generateVerificationToken(); // kept for backwards compatibility
        String verificationCode = generateVerificationCode();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        String resolvedCategory = resolveBusinessCategory(
                request.getBusinessCategory(), request.getCustomBusinessCategory(), tenantId);

        BusinessModel bm = null;
        if (request.getBusinessModel() != null && !request.getBusinessModel().isBlank()) {
            try { bm = BusinessModel.valueOf(request.getBusinessModel()); } catch (IllegalArgumentException ignored) {}
        }
        AnnualRevenueRange arr = null;
        if (request.getAnnualRevenueRange() != null && !request.getAnnualRevenueRange().isBlank()) {
            try { arr = AnnualRevenueRange.valueOf(request.getAnnualRevenueRange()); } catch (IllegalArgumentException ignored) {}
        }

        TenantOnboarding tenant = TenantOnboarding.builder()
            .tenantId(tenantId)
            .companyName(request.getCompanyName().trim())
            .legalBusinessName(request.getLegalBusinessName())
            .businessRegistrationNo(request.getBusinessRegistrationNo())
            .slug(slug)
            .email(normalizedEmail)
            .passwordHash(passwordHash)
            .businessCategory(resolvedCategory)
            .subCategory(request.getSubCategory())
            .businessModel(bm)
            .numberOfLocations(request.getNumberOfLocations())
            .identityMode(request.getIdentityMode())
            .subscriptionTier(SubscriptionTier.STANDARD)
            .dataResidencyRegion(request.getDataResidencyRegion())
            .countryCode(request.getCountryCode().toUpperCase())
            .headquartersAddress(request.getHeadquartersAddress())
            .founderNames(request.getFounderNames())
            .yearFounded(request.getYearFounded())
            .annualRevenueRange(arr)
            .customerBaseSize(request.getCustomerBaseSize())
            .paymentMethodsAccepted(request.getPaymentMethodsAccepted())
            .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
            .websiteUrl(request.getWebsiteUrl())
            .emailVerified(false)
            .emailVerificationToken(verificationToken)
            .emailVerificationExpiry(Instant.now().plus(24, ChronoUnit.HOURS))
            .emailVerificationCodeHash(passwordEncoder.encode(verificationCode))
            .emailVerificationCodeExpiry(Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES))
            .emailVerificationCodeAttempts(0)
            .emailVerificationCodeLastSentAt(Instant.now())
            .onboardingStatus(OnboardingStatus.PENDING_EMAIL_VERIFICATION)
            .build();

        TenantOnboarding saved;
        try {
            saved = tenantRepository.save(tenant);
        } catch (DataIntegrityViolationException e) {
            // Handles race-condition: another request created the same email concurrently.
            throw new DuplicateTenantException("email", "<redacted>");
        }

        // Create PRIMARY_ADMIN contact record
        contactRepository.save(TenantContact.builder()
            .tenantId(tenantId)
            .contactUid(UUID.randomUUID().toString())
            .name(request.getPrimaryContactName().trim())
            .email(request.getPrimaryContactEmail().toLowerCase().trim())
            .phone(request.getPrimaryContactPhone())
            .designation(request.getPrimaryContactDesignation())
            .role(ContactRole.PRIMARY_ADMIN)
            .isPrimary(true)
            .build());

        // Audit log for registration
        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(saved.getTenantId())
            .action("TENANT_REGISTERED")
            .actorRole("SELF")
            .actorId(tenantId)
            .afterState(Map.of(
                "email", normalizedEmail,
                "identityMode", request.getIdentityMode().name(),
                "businessCategory", resolvedCategory,
                "dataResidencyRegion", request.getDataResidencyRegion().name(),
                "slug", slug
            ))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        enforceOtpLimits(normalizedEmail, clientIp);
        emailVerificationMailer.sendVerificationCodeEmail(normalizedEmail, verificationCode);

        return TenantRegistrationResponse.builder()
            .tenantId(saved.getTenantId())
            .slug(saved.getSlug())
            .email(saved.getEmail())
            .onboardingStatus(saved.getOnboardingStatus())
            .identityMode(saved.getIdentityMode())
            .message("If this email can be registered, we sent a verification code.")
            .createdAt(saved.getCreatedAt())
            .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        TenantOnboarding tenant = tenantRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new TenantNotFoundException("Invalid or expired verification token"));

        if (tenant.getEmailVerificationExpiry().isBefore(Instant.now())) {
            throw new IllegalStateException("Verification token has expired. Please request a new one.");
        }

        tenant.setEmailVerified(true);
        tenant.setEmailVerificationToken(null);
        tenant.setEmailVerificationExpiry(null);

        stateMachine.transition(tenant, OnboardingStatus.EMAIL_VERIFIED, tenant.getTenantId(), "TENANT");
        tenantRepository.save(tenant);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request, String clientIp) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        TenantOnboarding tenant = tenantRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new TenantNotFoundException("Tenant not found for email: " + normalizedEmail));

        if (Boolean.TRUE.equals(tenant.getEmailVerified())) {
            throw new IllegalStateException("Email is already verified.");
        }

        if (tenant.getEmailVerificationCodeLastSentAt() != null) {
            Instant nextAllowed = tenant.getEmailVerificationCodeLastSentAt()
                .plus(VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS);
            if (Instant.now().isBefore(nextAllowed)) {
                throw new VerificationRateLimitException(
                    "Please wait a few seconds before requesting a new verification code.");
            }
        }

        String token = generateVerificationToken();
        String code = generateVerificationCode();
        tenant.setEmailVerificationToken(token);
        tenant.setEmailVerificationExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        tenant.setEmailVerificationCodeHash(passwordEncoder.encode(code));
        tenant.setEmailVerificationCodeExpiry(Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        tenant.setEmailVerificationCodeAttempts(0);
        tenant.setEmailVerificationCodeLastSentAt(Instant.now());

        tenantRepository.save(tenant);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenant.getTenantId())
            .action("EMAIL_VERIFICATION_RESENT")
            .actorId(tenant.getTenantId())
            .actorRole("TENANT")
            .afterState(Map.of("email", normalizedEmail))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));

        enforceOtpLimits(normalizedEmail, clientIp);
        emailVerificationMailer.sendVerificationCodeEmail(normalizedEmail, code);
    }

    private void enforceOtpLimits(String normalizedEmail, String clientIp) {
        // Per-email protection against OTP spam. Window starts at first send and lasts 24h.
        String key = OTP_SEND_PREFIX + normalizedEmail;
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1) {
            redis.expire(key, OTP_SEND_WINDOW);
        }
        if (current != null && current > OTP_MAX_SENDS_PER_24H) {
            throw new VerificationRateLimitException("Too many verification codes requested. Please try again later.");
        }

        // Per-IP protection (best-effort; behind proxies ensure X-Forwarded-For is set).
        if (clientIp != null && !clientIp.isBlank()) {
            String ipKey = OTP_SEND_IP_PREFIX + clientIp;
            Long ipCount = redis.opsForValue().increment(ipKey);
            if (ipCount != null && ipCount == 1) {
                redis.expire(ipKey, OTP_SEND_WINDOW);
            }
            if (ipCount != null && ipCount > OTP_MAX_SENDS_PER_IP_24H) {
                throw new VerificationRateLimitException("Too many verification codes requested. Please try again later.");
            }
        }
    }

    @Transactional
    public void verifyEmailCode(String email, String code, String newPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        TenantOnboarding tenant = tenantRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new TenantNotFoundException("Tenant not found for email: " + normalizedEmail));

        if (Boolean.TRUE.equals(tenant.getEmailVerified())) {
            return;
        }

        if (tenant.getEmailVerificationCodeHash() == null || tenant.getEmailVerificationCodeExpiry() == null) {
            throw new InvalidVerificationCodeException("Verification code not found. Please request a new code.");
        }

        if (tenant.getEmailVerificationCodeExpiry().isBefore(Instant.now())) {
            throw new InvalidVerificationCodeException("Verification code has expired. Please request a new code.");
        }

        if (tenant.getEmailVerificationCodeAttempts() != null
            && tenant.getEmailVerificationCodeAttempts() >= VERIFICATION_CODE_MAX_ATTEMPTS) {
            throw new InvalidVerificationCodeException("Too many invalid attempts. Please request a new code.");
        }

        boolean matches = passwordEncoder.matches(code, tenant.getEmailVerificationCodeHash());
        if (!matches) {
            int attempts = (tenant.getEmailVerificationCodeAttempts() == null ? 0 : tenant.getEmailVerificationCodeAttempts());
            tenant.setEmailVerificationCodeAttempts(attempts + 1);
            tenantRepository.save(tenant);
            throw new InvalidVerificationCodeException("Invalid verification code.");
        }

        tenant.setEmailVerified(true);
        if (newPassword != null && !newPassword.isBlank()) {
            tenant.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        tenant.setEmailVerificationToken(null);
        tenant.setEmailVerificationExpiry(null);
        tenant.setEmailVerificationCodeHash(null);
        tenant.setEmailVerificationCodeExpiry(null);
        tenant.setEmailVerificationCodeAttempts(0);
        tenant.setEmailVerificationCodeLastSentAt(null);

        stateMachine.transition(tenant, OnboardingStatus.EMAIL_VERIFIED, tenant.getTenantId(), "TENANT");
        tenantRepository.save(tenant);
    }

    @Transactional
    public void updateIdentity(String tenantId, UpdateIdentityRequest request) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        OnboardingStatus status = tenant.getOnboardingStatus();
        if (status != OnboardingStatus.EMAIL_VERIFIED
                && status != OnboardingStatus.AGREEMENT_PENDING
                && status != OnboardingStatus.AGREEMENT_SIGNED) {
            throw new IllegalStateException("Identity settings can only be updated after email verification.");
        }

        tenant.setIdentityMode(request.getIdentityMode());
        tenant.setDataResidencyRegion(request.getDataResidencyRegion());
        tenantRepository.save(tenant);

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("IDENTITY_UPDATED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of(
                "identityMode", request.getIdentityMode().name(),
                "dataResidencyRegion", request.getDataResidencyRegion().name()
            ))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));
    }

    @Transactional(readOnly = true)
    public TenantStatusResponse getStatus(String tenantId) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        var latestAgreement = agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
            .orElse(null);

        String approvalNotes = null;
        if (latestAgreement != null && latestAgreement.getStatus() == com.loyaltyos.onboarding.domain.enums.AgreementStatus.APPROVED) {
            approvalNotes = auditLogRepository
                .findTopByTenantIdAndActionOrderByCreatedAtDesc(tenantId, "AGREEMENT_APPROVED")
                .map(OnboardingAuditLog::getAfterState)
                .map(m -> m == null ? null : m.get("approvalNotes"))
                .map(v -> v == null ? null : String.valueOf(v).trim())
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
        }

        var primaryContact = contactRepository.findByTenantIdAndRole(tenantId, ContactRole.PRIMARY_ADMIN)
            .orElse(null);

        // Surface the moderation state of the tenant's resolved business category so the UI
        // can render an "awaiting review" / "rejected" hint without breaking the dropdown.
        RefBusinessCategory category = tenant.getBusinessCategory() != null
                ? businessCategoryRepository.findById(tenant.getBusinessCategory()).orElse(null)
                : null;

        return TenantStatusResponse.builder()
            .tenantId(tenant.getTenantId())
            .companyName(tenant.getCompanyName())
            .slug(tenant.getSlug())
            .email(tenant.getEmail())
            .onboardingStatus(tenant.getOnboardingStatus())
            .identityMode(tenant.getIdentityMode())
            .subscriptionTier(tenant.getSubscriptionTier())
            .dataResidencyRegion(tenant.getDataResidencyRegion())
            .emailVerified(tenant.getEmailVerified())
            .latestAgreementStatus(latestAgreement != null ? latestAgreement.getStatus() : null)
            .rejectionReason(latestAgreement != null ? latestAgreement.getRejectionReason() : null)
            .approvalNotes(approvalNotes)
            .createdAt(tenant.getCreatedAt())
            .activatedAt(tenant.getActivatedAt())
            .businessCategory(tenant.getBusinessCategory())
            .businessCategoryLabel(category != null ? category.getLabel() : null)
            .businessCategoryStatus(category != null && category.getStatus() != null
                    ? category.getStatus().name() : null)
            .businessCategoryDecisionReason(category != null ? category.getDecisionReason() : null)
            .countryCode(tenant.getCountryCode())
            .websiteUrl(tenant.getWebsiteUrl())
            .timezone(tenant.getTimezone())
            .primaryContactName(primaryContact != null ? primaryContact.getName() : null)
            .primaryContactEmail(primaryContact != null ? primaryContact.getEmail() : null)
            .primaryContactPhone(primaryContact != null ? primaryContact.getPhone() : null)
            .primaryContactDesignation(primaryContact != null ? primaryContact.getDesignation() : null)
            .legalBusinessName(tenant.getLegalBusinessName())
            .businessRegistrationNo(tenant.getBusinessRegistrationNo())
            .subCategory(tenant.getSubCategory())
            .businessModel(tenant.getBusinessModel() != null ? tenant.getBusinessModel().name() : null)
            .numberOfLocations(tenant.getNumberOfLocations())
            .headquartersAddress(tenant.getHeadquartersAddress())
            .founderNames(tenant.getFounderNames())
            .yearFounded(tenant.getYearFounded())
            .annualRevenueRange(tenant.getAnnualRevenueRange() != null ? tenant.getAnnualRevenueRange().name() : null)
            .customerBaseSize(tenant.getCustomerBaseSize())
            .paymentMethodsAccepted(tenant.getPaymentMethodsAccepted())
            .build();
    }

    @Transactional
    public void updateProfile(String tenantId, UpdateProfileRequest request) {
        TenantOnboarding tenant = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));

        OnboardingStatus status = tenant.getOnboardingStatus();
        if (status == OnboardingStatus.TERMINATED) {
            throw new IllegalStateException("Profile cannot be updated for a terminated tenant.");
        }

        String resolvedCategory = resolveBusinessCategory(
                request.getBusinessCategory(), request.getCustomBusinessCategory(), tenantId);

        BusinessModel bm = null;
        if (request.getBusinessModel() != null && !request.getBusinessModel().isBlank()) {
            try { bm = BusinessModel.valueOf(request.getBusinessModel()); } catch (IllegalArgumentException ignored) {}
        }
        AnnualRevenueRange arr = null;
        if (request.getAnnualRevenueRange() != null && !request.getAnnualRevenueRange().isBlank()) {
            try { arr = AnnualRevenueRange.valueOf(request.getAnnualRevenueRange()); } catch (IllegalArgumentException ignored) {}
        }

        tenant.setCompanyName(request.getCompanyName().trim());
        tenant.setLegalBusinessName(request.getLegalBusinessName());
        tenant.setBusinessRegistrationNo(request.getBusinessRegistrationNo());
        tenant.setBusinessCategory(resolvedCategory);
        tenant.setSubCategory(request.getSubCategory());
        tenant.setBusinessModel(bm);
        tenant.setNumberOfLocations(request.getNumberOfLocations());
        tenant.setCountryCode(request.getCountryCode().toUpperCase());
        tenant.setHeadquartersAddress(request.getHeadquartersAddress());
        tenant.setFounderNames(request.getFounderNames());
        tenant.setYearFounded(request.getYearFounded());
        tenant.setAnnualRevenueRange(arr);
        tenant.setCustomerBaseSize(request.getCustomerBaseSize());
        tenant.setPaymentMethodsAccepted(request.getPaymentMethodsAccepted());
        tenant.setWebsiteUrl(request.getWebsiteUrl());
        tenant.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        tenant.setUpdatedAt(Instant.now());
        tenantRepository.save(tenant);

        contactRepository.findByTenantIdAndRole(tenantId, ContactRole.PRIMARY_ADMIN)
            .ifPresent(contact -> {
                contact.setName(request.getPrimaryContactName().trim());
                contact.setEmail(request.getPrimaryContactEmail().toLowerCase().trim());
                contact.setPhone(request.getPrimaryContactPhone());
                contact.setDesignation(request.getPrimaryContactDesignation());
                contactRepository.save(contact);
            });

        OnboardingAuditLog audit = OnboardingAuditLog.builder()
            .tenantId(tenantId)
            .action("PROFILE_UPDATED")
            .actorId(tenantId)
            .actorRole("TENANT")
            .afterState(Map.of(
                "companyName", request.getCompanyName(),
                "businessCategory", resolvedCategory,
                "countryCode", request.getCountryCode()
            ))
            .build();
        auditLogRepository.save(Objects.requireNonNull(audit, "audit"));
    }

    /**
     * Resolves the {@code business_category} value to persist on the tenant record.
     *
     * <p>If the tenant picked a real industry from the dropdown, returns its code as-is.</p>
     *
     * <p>If the tenant picked {@code OTHER} and supplied a custom label, the label is sanitized to
     * a code. If a row already exists with that code, it is reused (regardless of moderation
     * status — this lets multiple tenants pile onto the same pending suggestion). Otherwise a new
     * row is inserted with {@link BusinessCategoryStatus#PENDING_REVIEW} so it does NOT show up in
     * the public dropdown until an admin approves it. The submitted label, original spelling and
     * the submitting tenant id are preserved for moderation.</p>
     *
     * <p>Existing seeded categories are unaffected — they were migrated to {@code APPROVED}.</p>
     */
    private String resolveBusinessCategory(String selectedCode, String customLabel, String submittedByTenantId) {
        if (!"OTHER".equalsIgnoreCase(selectedCode) || customLabel == null || customLabel.isBlank()) {
            return selectedCode.toUpperCase();
        }

        String trimmedLabel = customLabel.trim();
        String code = trimmedLabel
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (code.length() > 32) code = code.substring(0, 32);

        // Reuse existing row (any status) so we don't double-insert nor leak duplicate codes.
        if (businessCategoryRepository.existsById(code)) {
            return code;
        }

        int maxSort = businessCategoryRepository.findAll()
                .stream()
                .mapToInt(c -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                .max()
                .orElse(0);

        RefBusinessCategory newCat = new RefBusinessCategory();
        newCat.setCode(code);
        newCat.setLabel(trimmedLabel);
        newCat.setSortOrder(maxSort + 10);
        newCat.setActive(true);
        // Moderation: hidden from the public dropdown until an admin approves.
        newCat.setStatus(BusinessCategoryStatus.PENDING_REVIEW);
        newCat.setSubmittedByTenantId(submittedByTenantId);
        newCat.setSubmittedLabel(trimmedLabel);
        businessCategoryRepository.save(newCat);

        log.info("Industry suggestion submitted for review: code={}, label={}, byTenant={}",
                code, trimmedLabel, submittedByTenantId);
        return code;
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateVerificationCode() {
        int max = (int) Math.pow(10, VERIFICATION_CODE_LENGTH);
        int n = secureRandom.nextInt(max);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", n);
    }
}

