package com.loyaltyos.merchants.service;

import com.loyaltyos.access.exception.ModuleNotEntitledException;
import com.loyaltyos.access.service.AccessResolutionService;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.dto.MerchantAuthResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantCredentials;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.onboarding.exception.InvalidCredentialsException;
import com.loyaltyos.onboarding.security.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantAuthenticationService {

    private final MerchantRepository merchantRepository;
    private final MerchantCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final MerchantProperties merchantProperties;
    private final AccessResolutionService accessResolutionService;

    public MerchantAuthenticationService(
        MerchantRepository merchantRepository,
        MerchantCredentialsRepository credentialsRepository,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        JwtProperties jwtProperties,
        MerchantProperties merchantProperties,
        AccessResolutionService accessResolutionService
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.credentialsRepository = Objects.requireNonNull(credentialsRepository, "credentialsRepository");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.accessResolutionService = Objects.requireNonNull(accessResolutionService, "accessResolutionService");
    }

    @Transactional
    public MerchantCredentials issueCredentials(
        String tenantId,
        String merchantUid,
        String username,
        String plainPassword
    ) {
        credentialsRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .ifPresent(existing -> {
                existing.setActive(false);
                credentialsRepository.save(existing);
            });

        MerchantCredentials creds = new MerchantCredentials();
        creds.setCredentialUid(UUID.randomUUID().toString());
        creds.setTenantId(tenantId);
        creds.setMerchantUid(merchantUid);
        creds.setUsername(username.trim().toLowerCase());
        creds.setPasswordHash(passwordEncoder.encode(plainPassword));
        creds.setActive(true);
        creds.setLoginAttemptCount(0);
        return credentialsRepository.save(creds);
    }

    @Transactional
    public MerchantAuthResponse authenticate(String username, String password, String tenantId) {
        if (!merchantProperties.isEnabled()) {
            throw new InvalidCredentialsException();
        }
        if (!accessResolutionService.isModuleEntitled(tenantId, "merchants")) {
            throw new ModuleNotEntitledException("merchants");
        }
        String normalized = username.trim().toLowerCase();
        MerchantCredentials creds = credentialsRepository.findByUsernameAndTenantId(normalized, tenantId)
            .orElseThrow(InvalidCredentialsException::new);

        if (!creds.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (creds.isAccountLocked()) {
            throw new InvalidCredentialsException();
        }

        Merchant merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, creds.getMerchantUid())
            .orElseThrow(() -> new MerchantNotFoundException(creds.getMerchantUid()));
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(password, creds.getPasswordHash())) {
            creds.setLoginAttemptCount(creds.getLoginAttemptCount() + 1);
            if (creds.getLoginAttemptCount() >= merchantProperties.getMaxLoginAttempts()) {
                creds.setLockedUntil(Instant.now().plus(merchantProperties.getLockoutMinutes(), ChronoUnit.MINUTES));
            }
            credentialsRepository.save(creds);
            throw new InvalidCredentialsException();
        }

        creds.setLoginAttemptCount(0);
        creds.setLockedUntil(null);
        creds.setLastLoginAt(Instant.now());
        credentialsRepository.save(creds);

        return issueAccessToken(merchant, creds.getUsername());
    }

    public MerchantAuthResponse issueAccessToken(Merchant merchant, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60L);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiresAt(exp)
            .subject(merchant.getMerchantUid())
            .claim("tenantId", merchant.getTenantId())
            .claim("email", username)
            .claim("role", "MERCHANT")
            .claim("type", "merchant")
            .claim("merchantUid", merchant.getMerchantUid())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        MerchantAuthResponse response = new MerchantAuthResponse();
        response.setAccessToken(token);
        response.setExpiresInSeconds(exp.getEpochSecond() - now.getEpochSecond());
        response.setMerchantUid(merchant.getMerchantUid());
        response.setMerchantName(
            merchant.getDisplayName() != null ? merchant.getDisplayName() : merchant.getLegalName());
        response.setTenantId(merchant.getTenantId());
        return response;
    }
}
