package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.dto.request.LoginRequest;
import com.loyaltyos.onboarding.dto.response.LoginResponse;
import com.loyaltyos.onboarding.exception.EmailNotVerifiedException;
import com.loyaltyos.onboarding.exception.InvalidCredentialsException;
import com.loyaltyos.onboarding.repository.TenantAgreementRepository;
import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import com.loyaltyos.onboarding.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TenantAuthService {

    private final TenantOnboardingRepository tenantRepository;
    private final TenantAgreementRepository agreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public record AuthResult(LoginResponse response, String refreshToken) {}

    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        var tenant = tenantRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);

        boolean ok = passwordEncoder.matches(request.getPassword(), tenant.getPasswordHash());
        if (!ok) {
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(tenant.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }

        var latestAgreementStatus = agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
            .map(a -> a.getStatus())
            .orElse(null);

        LoginResponse access = issueAccessToken(
            tenant.getTenantId(),
            tenant.getEmail(),
            "TENANT_ADMIN",
            tenant.getOnboardingStatus(),
            latestAgreementStatus
        );

        String refresh = refreshTokenService.issue(
            new RefreshTokenService.RefreshPrincipal(tenant.getTenantId(), tenant.getEmail(), "TENANT_ADMIN")
        );

        return new AuthResult(access, refresh);
    }

    public AuthResult refresh(String refreshToken) {
        var principal = refreshTokenService.consumeAndRotate(refreshToken)
            .orElseThrow(InvalidCredentialsException::new);

        var tenant = tenantRepository.findByTenantId(principal.tenantId())
            .orElseThrow(InvalidCredentialsException::new);
        if (!Boolean.TRUE.equals(tenant.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }

        var latestAgreementStatus = agreementRepository.findTopByTenantIdOrderByCreatedAtDesc(tenant.getTenantId())
            .map(a -> a.getStatus())
            .orElse(null);

        LoginResponse access = issueAccessToken(
            tenant.getTenantId(),
            tenant.getEmail(),
            principal.role(),
            tenant.getOnboardingStatus(),
            latestAgreementStatus
        );

        String rotated = refreshTokenService.issue(
            new RefreshTokenService.RefreshPrincipal(tenant.getTenantId(), tenant.getEmail(), principal.role())
        );

        return new AuthResult(access, rotated);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private LoginResponse issueAccessToken(
        String tenantId,
        String email,
        String role,
        com.loyaltyos.onboarding.domain.enums.OnboardingStatus status,
        com.loyaltyos.onboarding.domain.enums.AgreementStatus latestAgreementStatus
    ) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiresAt(exp)
            .subject(tenantId)
            .claim("tenantId", tenantId)
            .claim("email", email)
            .claim("role", role)
            .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return LoginResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .expiresInSeconds(exp.getEpochSecond() - now.getEpochSecond())
            .tenantId(tenantId)
            .email(email)
            .onboardingStatus(status)
            .latestAgreementStatus(latestAgreementStatus)
            .build();
    }
}

