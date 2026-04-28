package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.domain.entity.AdminUser;
import com.loyaltyos.onboarding.dto.request.AdminLoginRequest;
import com.loyaltyos.onboarding.dto.response.AdminLoginResponse;
import com.loyaltyos.onboarding.exception.InvalidCredentialsException;
import com.loyaltyos.onboarding.repository.AdminUserRepository;
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
public class AdminAuthService {

    private final AdminUserRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AdminLoginResponse login(AdminLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        AdminUser admin = adminRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(admin.getIsActive())) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60L);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(admin.getAdminUid())
                .claim("adminUid", admin.getAdminUid())
                .claim("email", admin.getEmail())
                .claim("role", admin.getRole().name())
                .claim("type", "admin")
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return AdminLoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(exp.getEpochSecond() - now.getEpochSecond())
                .adminUid(admin.getAdminUid())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole())
                .build();
    }
}
