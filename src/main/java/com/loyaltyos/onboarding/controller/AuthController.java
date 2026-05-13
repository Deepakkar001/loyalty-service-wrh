package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.LoginRequest;
import com.loyaltyos.onboarding.dto.response.LoginResponse;
import com.loyaltyos.onboarding.security.JwtProperties;
import com.loyaltyos.onboarding.service.TenantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints for tenant admins")
public class AuthController {

    private final TenantAuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(TenantAuthService authService, JwtProperties jwtProperties) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties");
    }

    @PostMapping("/login")
    @Operation(summary = "Tenant admin login",
        description = "Returns a JWT access token for the tenant admin (email/password).")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()))
            .body(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
        description = "Rotates refresh token and returns a new access token. Refresh token is stored in HttpOnly cookie.")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String refreshToken = readCookie(request, jwtProperties.getRefreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            // Production hardening: reject refresh without cookie rather than passing null downstream.
            return ResponseEntity.status(401).build();
        }
        var result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()))
            .body(result.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes refresh token and clears cookie.")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = readCookie(request, jwtProperties.getRefreshCookieName());
        // Logout should be idempotent; if cookie missing, just clear cookie.
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
            .build();
    }

    private static String readCookie(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) return null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    @SuppressWarnings("null")
    private String refreshCookie(String rawToken) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(jwtProperties.getRefreshCookieName(), rawToken == null ? "" : rawToken)
            .httpOnly(true)
            .secure(jwtProperties.isSecureCookies())
            .path("/")
            .maxAge(java.time.Duration.ofDays(jwtProperties.getRefreshTtlDays()))
            // CSRF-hardening; adjust if you intentionally need cross-site.
            .sameSite(jwtProperties.getRefreshCookieSameSite());
        if (jwtProperties.getCookieDomain() != null && !jwtProperties.getCookieDomain().isBlank()) {
            b = b.domain(jwtProperties.getCookieDomain());
        }
        return b.build().toString();
    }

    @SuppressWarnings("null")
    private String clearRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
            .httpOnly(true)
            .secure(jwtProperties.isSecureCookies())
            .path("/")
            .maxAge(0)
            .sameSite(jwtProperties.getRefreshCookieSameSite());
        if (jwtProperties.getCookieDomain() != null && !jwtProperties.getCookieDomain().isBlank()) {
            b = b.domain(jwtProperties.getCookieDomain());
        }
        return b.build().toString();
    }
}

