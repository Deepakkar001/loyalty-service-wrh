package com.loyaltyos.onboarding.service;

import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.dto.MerchantAuthResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantCredentials;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.service.MerchantAuthenticationService;
import com.loyaltyos.onboarding.dto.LoginRequest;
import com.loyaltyos.onboarding.dto.SignInOrganisationOption;
import com.loyaltyos.onboarding.dto.UnifiedSignInRequest;
import com.loyaltyos.onboarding.dto.UnifiedSignInResponse;
import com.loyaltyos.onboarding.exception.InvalidCredentialsException;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedAuthService {

    private final TenantAuthService tenantAuthService;
    private final MerchantAuthenticationService merchantAuthenticationService;
    private final TenantUserRepository tenantUserRepository;
    private final TenantOnboardingRepository tenantRepository;
    private final MerchantCredentialsRepository merchantCredentialsRepository;
    private final MerchantRepository merchantRepository;
    private final AccessResolutionService accessResolutionService;
    private final MerchantProperties merchantProperties;
    private final PasswordEncoder passwordEncoder;

    public UnifiedAuthService(
        TenantAuthService tenantAuthService,
        MerchantAuthenticationService merchantAuthenticationService,
        TenantUserRepository tenantUserRepository,
        TenantOnboardingRepository tenantRepository,
        MerchantCredentialsRepository merchantCredentialsRepository,
        MerchantRepository merchantRepository,
        AccessResolutionService accessResolutionService,
        MerchantProperties merchantProperties,
        PasswordEncoder passwordEncoder
    ) {
        this.tenantAuthService = Objects.requireNonNull(tenantAuthService, "tenantAuthService");
        this.merchantAuthenticationService = Objects.requireNonNull(
            merchantAuthenticationService, "merchantAuthenticationService");
        this.tenantUserRepository = Objects.requireNonNull(tenantUserRepository, "tenantUserRepository");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.merchantCredentialsRepository = Objects.requireNonNull(
            merchantCredentialsRepository, "merchantCredentialsRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.accessResolutionService = Objects.requireNonNull(
            accessResolutionService, "accessResolutionService");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    public record UnifiedSignInResult(UnifiedSignInResponse response, String refreshToken) {}

    /**
     * Orchestrates tenant vs merchant sign-in without joining their transactions.
     * A failed tenant password attempt must not mark this flow rollback-only before merchant auth runs.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UnifiedSignInResult signIn(UnifiedSignInRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();
        String tenantId = normalizeTenantId(request.getTenantId());

        if (tenantId != null) {
            if (!merchantProperties.isEnabled()) {
                throw new InvalidCredentialsException();
            }
            MerchantAuthResponse merchant = merchantAuthenticationService.authenticate(email, password, tenantId);
            return new UnifiedSignInResult(UnifiedSignInResponse.merchant(merchant), null);
        }

        if (isTenantAccount(email)) {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword(password);
            try {
                TenantAuthService.AuthResult tenantResult = tenantAuthService.login(loginRequest);
                return new UnifiedSignInResult(
                    UnifiedSignInResponse.tenant(tenantResult.response()),
                    tenantResult.refreshToken()
                );
            } catch (InvalidCredentialsException ex) {
                if (!hasMerchantPortalAccount(email)) {
                    throw ex;
                }
                // Same email may be tenant admin and merchant partner in dev; try merchant password next.
            }
        }

        if (!merchantProperties.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        List<SignInOrganisationOption> eligible = findEligibleMerchantOrganisations(email, password);
        if (eligible.isEmpty()) {
            if (hasPendingMerchantInvite(email)) {
                throw new InvalidCredentialsException(
                    "Activate your merchant account using the invite link before signing in.");
            }
            throw new InvalidCredentialsException();
        }
        if (eligible.size() == 1) {
            SignInOrganisationOption only = eligible.get(0);
            MerchantAuthResponse merchant = merchantAuthenticationService.authenticate(
                email, password, only.getTenantId());
            return new UnifiedSignInResult(UnifiedSignInResponse.merchant(merchant), null);
        }

        return new UnifiedSignInResult(
            UnifiedSignInResponse.organisationSelection(eligible),
            null
        );
    }

    private boolean hasMerchantPortalAccount(String email) {
        return merchantCredentialsRepository.findByUsernameIgnoreCaseAndActiveTrue(email).stream()
            .anyMatch(creds -> !creds.isInvitePending());
    }

    private boolean hasPendingMerchantInvite(String email) {
        return merchantCredentialsRepository.findByUsernameIgnoreCaseAndActiveTrue(email).stream()
            .anyMatch(MerchantCredentials::isInvitePending);
    }

    private boolean isTenantAccount(String email) {
        return tenantUserRepository.findByEmailIgnoreCase(email).isPresent()
            || tenantRepository.findByEmail(email).isPresent();
    }

    /**
     * Resolves merchant organisations where the password is valid without incrementing lockout counters.
     * Final authentication (with audit + last-login) happens via {@link MerchantAuthenticationService#authenticate}.
     */
    private List<SignInOrganisationOption> findEligibleMerchantOrganisations(String email, String password) {
        List<MerchantCredentials> credentials =
            merchantCredentialsRepository.findByUsernameIgnoreCaseAndActiveTrue(email);

        Map<String, SignInOrganisationOption> uniqueByTenant = new LinkedHashMap<>();
        for (MerchantCredentials creds : credentials) {
            if (creds.isInvitePending() || creds.isAccountLocked()) {
                continue;
            }
            if (!passwordEncoder.matches(password, creds.getPasswordHash())) {
                continue;
            }
            if (!accessResolutionService.isModuleEntitled(creds.getTenantId(), "merchants")) {
                continue;
            }

            Merchant merchant = merchantRepository
                .findByTenantIdAndMerchantUid(creds.getTenantId(), creds.getMerchantUid())
                .orElse(null);
            if (merchant == null || merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
                continue;
            }

            String merchantName = merchant.getDisplayName() != null
                ? merchant.getDisplayName()
                : merchant.getLegalName();
            uniqueByTenant.putIfAbsent(
                creds.getTenantId(),
                new SignInOrganisationOption(creds.getTenantId(), creds.getMerchantUid(), merchantName)
            );
        }

        return new ArrayList<>(uniqueByTenant.values());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return tenantId.trim();
    }
}
