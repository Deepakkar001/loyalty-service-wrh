package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.SubmitAgreementRequest;
import com.loyaltyos.onboarding.dto.request.UpdateIdentityRequest;
import com.loyaltyos.onboarding.dto.request.UpdateProfileRequest;
import com.loyaltyos.onboarding.dto.response.TenantStatusResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.TenantAgreementService;
import com.loyaltyos.onboarding.service.TenantRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Tenant-scoped endpoints (JWT required)")
public class MeController {

    private final TenantRegistrationService registrationService;
    private final TenantAgreementService agreementService;

    @GetMapping("/status")
    @Operation(summary = "Get my tenant onboarding status",
        description = "Returns onboarding status for the authenticated tenant.")
    public ResponseEntity<TenantStatusResponse> myStatus(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(registrationService.getStatus(tenantId));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update tenant profile (Step 1 fields)",
        description = "Updates editable profile fields for the authenticated tenant.")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody UpdateProfileRequest request) {
        String tenantId = TenantJwt.tenantId(jwt);
        registrationService.updateProfile(tenantId, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/identity")
    @Operation(summary = "Update identity mode and data residency (Step 2)",
        description = "Persists identity mode and data residency region for the authenticated tenant.")
    public ResponseEntity<Void> updateIdentity(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody UpdateIdentityRequest request) {
        String tenantId = TenantJwt.tenantId(jwt);
        registrationService.updateIdentity(tenantId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/agreement")
    @Operation(summary = "Submit my commercial agreement (Stage 3)",
        description = "Persists signed terms for the authenticated tenant.")
    public ResponseEntity<Void> submitAgreement(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody SubmitAgreementRequest request) {
        String tenantId = TenantJwt.tenantId(jwt);
        agreementService.submitAgreement(tenantId, request);
        return ResponseEntity.accepted().build();
    }
}

