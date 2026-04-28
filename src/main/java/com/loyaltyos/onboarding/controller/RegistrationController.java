package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.RegisterTenantRequest;
import com.loyaltyos.onboarding.dto.request.ResendVerificationRequest;
import com.loyaltyos.onboarding.dto.request.VerifyEmailCodeRequest;
import com.loyaltyos.onboarding.dto.response.TenantRegistrationResponse;
import com.loyaltyos.onboarding.dto.response.TenantStatusResponse;
import com.loyaltyos.onboarding.service.TenantRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Tenant Onboarding", description = "Tenant registration and onboarding workflow")
public class RegistrationController {

    private final TenantRegistrationService registrationService;
    private final com.loyaltyos.onboarding.service.IdempotencyService idempotencyService;

    @PostMapping("/register")
    @Operation(summary = "Register a new tenant",
               description = "Stage 1: Creates tenant account and sends email verification")
    public ResponseEntity<TenantRegistrationResponse> register(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RegisterTenantRequest request,
            HttpServletRequest httpRequest) {

        var cached = idempotencyService.getCachedResponse(idempotencyKey, TenantRegistrationResponse.class);
        if (cached.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(cached.get());
        }

        TenantRegistrationResponse response = registrationService.register(request, clientIp(httpRequest));
        idempotencyService.cacheResponse(idempotencyKey, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify tenant email address",
               description = "Stage 1: Confirms email using the token sent in the verification email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        registrationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Stage 1 — Resend verification email",
               description = "Regenerates and resends the email verification token if not yet verified")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request, HttpServletRequest httpRequest) {
        registrationService.resendVerification(request, clientIp(httpRequest));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email-code")
    @Operation(summary = "Verify tenant email using a code",
        description = "Stage 1: Confirms email using the 6-digit code sent via email")
    public ResponseEntity<Void> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        registrationService.verifyEmailCode(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tenantId}/status")
    @Operation(summary = "Get current onboarding status",
               description = "Returns the current stage and status of the tenant's onboarding journey")
    public ResponseEntity<TenantStatusResponse> getStatus(@PathVariable String tenantId) {
        return ResponseEntity.ok(registrationService.getStatus(tenantId));
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}

