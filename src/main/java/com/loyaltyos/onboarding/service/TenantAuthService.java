package com.loyaltyos.onboarding.service;

import com.loyaltyos.access.entity.TenantUser;
import com.loyaltyos.access.enums.TenantUserStatus;
import com.loyaltyos.access.repository.TenantUserRepository;
import com.loyaltyos.access.service.AccessProvisioningService;
import com.loyaltyos.onboarding.dto.AcceptInviteRequest;
import com.loyaltyos.onboarding.dto.LoginRequest;
import com.loyaltyos.onboarding.dto.LoginResponse;
import com.loyaltyos.onboarding.exception.EmailNotVerifiedException;
import com.loyaltyos.onboarding.exception.InvalidCredentialsException;
import com.loyaltyos.onboarding.enums.ContactRole;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantContactRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.security.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class TenantAuthService {

    private final TenantOnboardingRepository tenantRepository;
    private final TenantContactRepository contactRepository;
    private final TenantAgreementRepository agreementRepository;
    private final TenantUserRepository tenantUserRepository;
    private final AccessProvisioningService accessProvisioningService;
    private final com.loyaltyos.access.service.TenantUserAdminService tenantUserAdminService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public TenantAuthService(
        TenantOnboardingRepository tenantRepository,
        TenantContactRepository contactRepository,
        TenantAgreementRepository agreementRepository,
        TenantUserRepository tenantUserRepository,
        AccessProvisioningService accessProvisioningService,
        com.loyaltyos.access.service.TenantUserAdminService tenantUserAdminService,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        JwtProperties jwtProperties,
        RefreshTokenService refreshTokenService
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.contactRepository = Objects.requireNonNull(contactRepository, "contactRepository");
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.tenantUserRepository = Objects.requireNonNull(tenantUserRepository, "tenantUserRepository");
        this.accessProvisioningService = Objects.requireNonNull(accessProvisioningService, "accessProvisioningService");
        this.tenantUserAdminService = Objects.requireNonNull(tenantUserAdminService, "tenantUserAdminService");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
        this.refreshTokenService = Objects.requireNonNull(refreshTokenService, "refreshTokenService");
    }

    public record AuthResult(LoginResponse response, String refreshToken) {}

    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        var tenant = tenantRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(tenant.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }

        TenantUser user = resolveOrProvisionUser(tenant.getTenantId(), email, tenant.getPasswordHash());

        boolean ok = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!ok) {
            ok = passwordEncoder.matches(request.getPassword(), tenant.getPasswordHash());
            if (ok) {
                user.setPasswordHash(tenant.getPasswordHash());
                tenantUserRepository.save(user);
            }
        }
        if (!ok) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == TenantUserStatus.DISABLED || user.getStatus() == TenantUserStatus.INVITED) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(Instant.now());
        tenantUserRepository.save(user);

        var latestAgreementStatus = agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
            .map(a -> a.getStatus())
            .orElse(null);

        String fullName = user.getFullName() != null ? user.getFullName() : resolveFullName(tenant.getTenantId());

        LoginResponse access = issueAccessToken(
            tenant.getTenantId(),
            user.getUserId(),
            tenant.getEmail(),
            fullName,
            "TENANT_ADMIN",
            user.getSessionVersion(),
            tenant.getOnboardingStatus(),
            latestAgreementStatus
        );

        String refresh = refreshTokenService.issue(
            new RefreshTokenService.RefreshPrincipal(
                tenant.getTenantId(), tenant.getEmail(), "TENANT_ADMIN", user.getUserId())
        );

        return new AuthResult(access, refresh);
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        var principal = refreshTokenService.consumeAndRotate(refreshToken)
            .orElseThrow(InvalidCredentialsException::new);

        var tenant = tenantRepository.findByTenantId(principal.tenantId())
            .orElseThrow(InvalidCredentialsException::new);
        if (!Boolean.TRUE.equals(tenant.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }

        TenantUser user = resolveUser(principal);
        if (user.getStatus() == TenantUserStatus.DISABLED) {
            throw new InvalidCredentialsException();
        }

        var latestAgreementStatus = agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
            .map(a -> a.getStatus())
            .orElse(null);

        String fullName = user.getFullName() != null ? user.getFullName() : resolveFullName(tenant.getTenantId());

        LoginResponse access = issueAccessToken(
            tenant.getTenantId(),
            user.getUserId(),
            tenant.getEmail(),
            fullName,
            principal.role(),
            user.getSessionVersion(),
            tenant.getOnboardingStatus(),
            latestAgreementStatus
        );

        String rotated = refreshTokenService.issue(
            new RefreshTokenService.RefreshPrincipal(
                tenant.getTenantId(), tenant.getEmail(), principal.role(), user.getUserId())
        );

        return new AuthResult(access, rotated);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public AuthResult acceptInvite(AcceptInviteRequest request) {
        tenantUserAdminService.acceptInvite(request.getEmail(), request.getToken(), request.getPassword());
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        return login(loginRequest);
    }

    private TenantUser resolveUser(RefreshTokenService.RefreshPrincipal principal) {
        if (principal.tenantUserId() != null && !principal.tenantUserId().isBlank()) {
            return tenantUserRepository.findById(principal.tenantUserId())
                .filter(u -> principal.tenantId().equals(u.getTenantId()))
                .orElseGet(() -> resolveOrProvisionUser(
                    principal.tenantId(), principal.email(),
                    tenantRepository.findByTenantId(principal.tenantId())
                        .map(t -> t.getPasswordHash()).orElse(null)));
        }
        return tenantUserRepository.findByEmailIgnoreCase(principal.email())
            .filter(u -> principal.tenantId().equals(u.getTenantId()))
            .orElseGet(() -> resolveOrProvisionUser(
                principal.tenantId(), principal.email(),
                tenantRepository.findByTenantId(principal.tenantId())
                    .map(t -> t.getPasswordHash()).orElse(null)));
    }

    private TenantUser resolveOrProvisionUser(String tenantId, String email, String passwordHash) {
        return tenantUserRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
            .orElseGet(() -> {
                String fullName = resolveFullName(tenantId);
                return accessProvisioningService.provisionPrimaryAdmin(tenantId, email, passwordHash, fullName);
            });
    }

    private String resolveFullName(String tenantId) {
        return contactRepository.findByTenantIdAndRole(tenantId, ContactRole.PRIMARY_ADMIN)
            .map(contact -> contact.getName())
            .orElse(null);
    }

    private LoginResponse issueAccessToken(
        String tenantId,
        String tenantUserId,
        String email,
        String fullName,
        String role,
        int sessionVersion,
        com.loyaltyos.onboarding.enums.OnboardingStatus status,
        com.loyaltyos.onboarding.enums.AgreementStatus latestAgreementStatus
    ) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiresAt(exp)
            .subject(tenantId)
            .claim("tenantId", tenantId)
            .claim("tenantUserId", tenantUserId)
            .claim("email", email)
            .claim("role", role)
            .claim("sessionVersion", sessionVersion)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return LoginResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresInSeconds(exp.getEpochSecond() - now.getEpochSecond())
            .tenantId(tenantId)
            .email(email)
            .fullName(fullName)
            .onboardingStatus(status)
            .latestAgreementStatus(latestAgreementStatus)
            .build();
    }
}
