package com.loyaltyos.support.controller;

import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.support.dto.CreateSupportCaseRequest;
import com.loyaltyos.support.dto.SupportCaseResponse;
import com.loyaltyos.support.dto.SupportContextResponse;
import com.loyaltyos.support.dto.UpdateSupportCaseStatusRequest;
import com.loyaltyos.support.service.SupportCaseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/support")
public class SupportController {

    private final SupportCaseService supportCaseService;

    public SupportController(SupportCaseService supportCaseService) {
        this.supportCaseService = Objects.requireNonNull(supportCaseService);
    }

    @GetMapping("/context")
    public ResponseEntity<SupportContextResponse> context(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(supportCaseService.getContext(jwt));
    }

    @GetMapping("/cases")
    public ResponseEntity<List<SupportCaseResponse>> listCases(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        return ResponseEntity.ok(supportCaseService.listCasesForTenant(tenantId));
    }

    @GetMapping("/cases/{caseUid}")
    public ResponseEntity<SupportCaseResponse> getCase(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String caseUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        return ResponseEntity.ok(supportCaseService.getCaseForTenant(tenantId, caseUid));
    }

    @PatchMapping("/cases/{caseUid}/status")
    public ResponseEntity<SupportCaseResponse> updateStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String caseUid,
        @Valid @RequestBody UpdateSupportCaseStatusRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        return ResponseEntity.ok(supportCaseService.updateStatusForTenant(tenantId, caseUid, request, jwt));
    }

    @PostMapping("/cases")
    public ResponseEntity<SupportCaseResponse> createCase(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateSupportCaseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportCaseService.createCase(jwt, request));
    }
}
