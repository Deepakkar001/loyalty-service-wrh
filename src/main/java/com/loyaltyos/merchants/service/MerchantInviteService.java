package com.loyaltyos.merchants.service;

import com.loyaltyos.access.security.TenantPasswordPolicy;
import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.dto.MerchantInviteAcceptResponse;
import com.loyaltyos.merchants.dto.MerchantInviteValidateResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantCredentials;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantCredentialsRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantInviteService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MerchantCredentialsRepository credentialsRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantInviteMailer inviteMailer;
    private final MerchantProperties merchantProperties;
    private final PasswordEncoder passwordEncoder;

    public MerchantInviteService(
        MerchantCredentialsRepository credentialsRepository,
        MerchantRepository merchantRepository,
        MerchantInviteMailer inviteMailer,
        MerchantProperties merchantProperties,
        PasswordEncoder passwordEncoder
    ) {
        this.credentialsRepository = Objects.requireNonNull(credentialsRepository, "credentialsRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.inviteMailer = Objects.requireNonNull(inviteMailer, "inviteMailer");
        this.merchantProperties = Objects.requireNonNull(merchantProperties, "merchantProperties");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
    }

    @Transactional
    public InviteIssueResult issuePortalInvite(Merchant merchant) {
        return issuePortalInvite(merchant, true);
    }

    @Transactional
    public InviteIssueResult issuePortalInvite(Merchant merchant, boolean sendEmail) {
        String email = merchant.getContactEmail().trim().toLowerCase();
        deactivateExistingCredentials(merchant.getTenantId(), merchant.getMerchantUid());

        MerchantCredentials creds = new MerchantCredentials();
        creds.setCredentialUid(UUID.randomUUID().toString());
        creds.setTenantId(merchant.getTenantId());
        creds.setMerchantUid(merchant.getMerchantUid());
        creds.setUsername(email);
        creds.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        creds.setActive(true);
        creds.setMustChangePassword(false);
        creds.setLoginAttemptCount(0);

        String plainToken = generateToken();
        creds.setInviteTokenHash(hashToken(plainToken));
        creds.setInviteTokenExpiresAt(
            Instant.now().plus(merchantProperties.getInviteTokenTtlDays(), ChronoUnit.DAYS)
        );
        credentialsRepository.save(creds);

        String merchantName = merchant.getDisplayName() != null
            ? merchant.getDisplayName()
            : merchant.getLegalName();
        boolean emailSent = false;
        if (sendEmail) {
            emailSent = inviteMailer.sendInviteEmail(email, merchantName, plainToken);
        }
        String inviteUrl = inviteMailer.buildAcceptUrl(plainToken);
        return new InviteIssueResult(creds, emailSent, inviteUrl, creds.getInviteTokenExpiresAt());
    }

    @Transactional
    public InviteIssueResult resendPortalInvite(Merchant merchant) {
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Portal invite can only be resent for active merchants"
            );
        }
        if (merchant.getContactEmail() == null || merchant.getContactEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant contact email is required");
        }

        return credentialsRepository
            .findByTenantIdAndMerchantUid(merchant.getTenantId(), merchant.getMerchantUid())
            .filter(MerchantCredentials::isActive)
            .map(creds -> {
                if (creds.getInviteAcceptedAt() != null) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Merchant has already accepted the portal invite"
                    );
                }
                String plainToken = generateToken();
                creds.setInviteTokenHash(hashToken(plainToken));
                creds.setInviteTokenExpiresAt(
                    Instant.now().plus(merchantProperties.getInviteTokenTtlDays(), ChronoUnit.DAYS)
                );
                creds.setLoginAttemptCount(0);
                creds.setLockedUntil(null);
                credentialsRepository.save(creds);

                String merchantName = merchant.getDisplayName() != null
                    ? merchant.getDisplayName()
                    : merchant.getLegalName();
                boolean emailSent = inviteMailer.sendInviteEmail(
                    creds.getUsername(),
                    merchantName,
                    plainToken
                );
                String inviteUrl = inviteMailer.buildAcceptUrl(plainToken);
                return new InviteIssueResult(creds, emailSent, inviteUrl, creds.getInviteTokenExpiresAt());
            })
            .orElseGet(() -> issuePortalInvite(merchant));
    }

    /**
     * Refresh invite token and return portal URL without sending email (SMTP fallback for ops).
     */
    @Transactional
    public InviteIssueResult issuePortalInviteLink(Merchant merchant) {
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Portal invite link can only be issued for active merchants"
            );
        }
        if (merchant.getContactEmail() == null || merchant.getContactEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Merchant contact email is required");
        }

        return credentialsRepository
            .findByTenantIdAndMerchantUid(merchant.getTenantId(), merchant.getMerchantUid())
            .filter(MerchantCredentials::isActive)
            .map(creds -> {
                if (creds.getInviteAcceptedAt() != null) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Merchant has already accepted the portal invite"
                    );
                }
                String plainToken = generateToken();
                creds.setInviteTokenHash(hashToken(plainToken));
                creds.setInviteTokenExpiresAt(
                    Instant.now().plus(merchantProperties.getInviteTokenTtlDays(), ChronoUnit.DAYS)
                );
                credentialsRepository.save(creds);
                String inviteUrl = inviteMailer.buildAcceptUrl(plainToken);
                return new InviteIssueResult(creds, false, inviteUrl, creds.getInviteTokenExpiresAt());
            })
            .orElseGet(() -> issuePortalInvite(merchant, false));
    }

    @Transactional(readOnly = true)
    public MerchantInviteValidateResponse validateToken(String token) {
        MerchantInviteValidateResponse response = new MerchantInviteValidateResponse();
        if (token == null || token.isBlank()) {
            response.setValid(false);
            return response;
        }
        return credentialsRepository.findByInviteTokenHashAndActiveTrue(hashToken(token.trim()))
            .filter(this::isInviteUsable)
            .map(creds -> {
                Merchant merchant = merchantRepository
                    .findByTenantIdAndMerchantUid(creds.getTenantId(), creds.getMerchantUid())
                    .orElse(null);
                response.setValid(true);
                response.setEmailMasked(maskEmail(creds.getUsername()));
                response.setMerchantName(merchant != null
                    ? (merchant.getDisplayName() != null ? merchant.getDisplayName() : merchant.getLegalName())
                    : "Merchant");
                if (creds.getInviteTokenExpiresAt() != null) {
                    response.setExpiresAt(creds.getInviteTokenExpiresAt().toString());
                }
                return response;
            })
            .orElseGet(() -> {
                response.setValid(false);
                return response;
            });
    }

    @Transactional
    public MerchantInviteAcceptResponse acceptInvite(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite token is required");
        }
        MerchantCredentials creds = credentialsRepository
            .findByInviteTokenHashAndActiveTrue(hashToken(token.trim()))
            .filter(this::isInviteUsable)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired invite link"));

        TenantPasswordPolicy.validateNewPassword(newPassword, creds.getUsername(), null);
        creds.setPasswordHash(passwordEncoder.encode(newPassword));
        creds.setInviteTokenHash(null);
        creds.setInviteTokenExpiresAt(null);
        creds.setInviteAcceptedAt(Instant.now());
        creds.setMustChangePassword(false);
        creds.setLoginAttemptCount(0);
        creds.setLockedUntil(null);
        credentialsRepository.save(creds);

        MerchantInviteAcceptResponse response = new MerchantInviteAcceptResponse();
        response.setSuccess(true);
        return response;
    }

    private void deactivateExistingCredentials(String tenantId, String merchantUid) {
        credentialsRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .ifPresent(existing -> {
                existing.setActive(false);
                credentialsRepository.save(existing);
            });
    }

    private boolean isInviteUsable(MerchantCredentials creds) {
        if (!creds.isInvitePending()) {
            return false;
        }
        Instant expires = creds.getInviteTokenExpiresAt();
        return expires == null || expires.isAfter(Instant.now());
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2
            ? local.charAt(0) + "***"
            : local.substring(0, 2) + "***";
        return maskedLocal + "@" + parts[1];
    }

    public record InviteIssueResult(
        MerchantCredentials credentials,
        boolean emailSent,
        String inviteUrl,
        Instant expiresAt
    ) {}
}
