package com.loyaltyos.merchants.service;

import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.merchants.dto.MerchantEmailAvailabilityResponse;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ensures merchant portal login emails do not collide with tenant admins, tenant users,
 * or other merchants under the same tenant.
 */
@Service
public class MerchantOnboardingEmailGuard {

    private final MerchantRepository merchantRepository;
    private final MerchantCredentialsRepository credentialsRepository;
    private final TenantOnboardingRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;

    public MerchantOnboardingEmailGuard(
        MerchantRepository merchantRepository,
        MerchantCredentialsRepository credentialsRepository,
        TenantOnboardingRepository tenantRepository,
        TenantUserRepository tenantUserRepository
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.credentialsRepository = Objects.requireNonNull(credentialsRepository, "credentialsRepository");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.tenantUserRepository = Objects.requireNonNull(tenantUserRepository, "tenantUserRepository");
    }

    /** Validates email used for merchant portal login (contact email). */
    public void assertPortalEmailAvailable(String tenantId, String email, String excludeMerchantUid) {
        MerchantEmailAvailabilityResponse result = checkPortalEmailAvailability(tenantId, email, excludeMerchantUid);
        if (!result.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, result.getMessage());
        }
    }

    /** @deprecated use {@link #assertPortalEmailAvailable} */
    @Deprecated
    public void assertAvailableForMerchantOnboarding(String tenantId, String email, String excludeMerchantUid) {
        assertPortalEmailAvailable(tenantId, email, excludeMerchantUid);
    }

    public MerchantEmailAvailabilityResponse checkPortalEmailAvailability(
        String tenantId,
        String email,
        String excludeMerchantUid
    ) {
        String normalized = normalize(email);
        if (normalized == null || normalized.isBlank()) {
            return MerchantEmailAvailabilityResponse.unavailable("Email is required");
        }

        return findPortalEmailConflict(tenantId, normalized, excludeMerchantUid)
            .map(MerchantEmailAvailabilityResponse::unavailable)
            .orElseGet(MerchantEmailAvailabilityResponse::available);
    }

    /** @deprecated use {@link #checkPortalEmailAvailability} */
    @Deprecated
    public MerchantEmailAvailabilityResponse checkAvailability(
        String tenantId,
        String email,
        String excludeMerchantUid
    ) {
        return checkPortalEmailAvailability(tenantId, email, excludeMerchantUid);
    }

    private Optional<String> findPortalEmailConflict(String tenantId, String normalizedEmail, String excludeMerchantUid) {
        if (isTenantPrimaryAdminEmail(tenantId, normalizedEmail)) {
            return Optional.of("This email belongs to your tenant administrator account and cannot be used for a merchant.");
        }
        if (tenantUserRepository.findByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail).isPresent()) {
            return Optional.of("This email is already assigned to a tenant user in your organisation.");
        }
        if (isUsedByAnotherMerchant(tenantId, normalizedEmail, excludeMerchantUid)) {
            return Optional.of("This email is already used by another merchant in your programme.");
        }
        if (isUsedByAnotherMerchantPortalLogin(tenantId, normalizedEmail, excludeMerchantUid)) {
            return Optional.of("This email is already linked to another merchant portal account.");
        }
        return Optional.empty();
    }

    private boolean isTenantPrimaryAdminEmail(String tenantId, String normalizedEmail) {
        return tenantRepository.findByTenantId(tenantId)
            .map(tenant -> normalizedEmail.equalsIgnoreCase(normalize(tenant.getEmail())))
            .orElse(false);
    }

    private boolean isUsedByAnotherMerchant(String tenantId, String normalizedEmail, String excludeMerchantUid) {
        if (excludeMerchantUid == null || excludeMerchantUid.isBlank()) {
            return merchantRepository.existsByTenantIdAndContactEmailIgnoreCase(tenantId, normalizedEmail);
        }
        return merchantRepository.existsByTenantIdAndContactEmailIgnoreCaseAndMerchantUidNot(
            tenantId, normalizedEmail, excludeMerchantUid.trim());
    }

    private boolean isUsedByAnotherMerchantPortalLogin(
        String tenantId,
        String normalizedEmail,
        String excludeMerchantUid
    ) {
        if (excludeMerchantUid == null || excludeMerchantUid.isBlank()) {
            return credentialsRepository.existsByTenantIdAndUsernameIgnoreCase(tenantId, normalizedEmail);
        }
        return credentialsRepository.existsByTenantIdAndUsernameIgnoreCaseAndMerchantUidNot(
            tenantId, normalizedEmail, excludeMerchantUid.trim());
    }

    static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
