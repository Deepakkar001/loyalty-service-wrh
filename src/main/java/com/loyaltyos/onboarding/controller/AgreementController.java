package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.SubmitAgreementRequest;
import com.loyaltyos.onboarding.service.TenantAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Tenant Onboarding", description = "Tenant registration and onboarding workflow")
public class AgreementController {

    private final TenantAgreementService agreementService;

    @PostMapping("/{tenantId}/agreement")
    @Operation(summary = "Stage 3 — Submit commercial agreement",
        description = "Persists signed commercial terms and transitions onboarding status to AGREEMENT_SIGNED")
    public ResponseEntity<Void> submitAgreement(
        @PathVariable String tenantId,
        @Valid @RequestBody SubmitAgreementRequest request
    ) {
        agreementService.submitAgreement(tenantId, request);
        return ResponseEntity.accepted().build();
    }
}

