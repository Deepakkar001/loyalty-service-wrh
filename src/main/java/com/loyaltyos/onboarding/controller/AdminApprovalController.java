package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.dto.request.ApproveAgreementRequest;
import com.loyaltyos.onboarding.dto.request.RejectAgreementRequest;
import com.loyaltyos.onboarding.dto.response.PendingAgreementListItem;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.AgreementApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/agreements")
@RequiredArgsConstructor
@Tag(name = "Admin Approval", description = "Maker-checker agreement approval workflow")
public class AdminApprovalController {

    private final AgreementApprovalService approvalService;

    @GetMapping("/pending")
    @Operation(summary = "List pending agreements", description = "Returns all agreements awaiting admin approval")
    public ResponseEntity<List<PendingAgreementListItem>> listPending() {
        return ResponseEntity.ok(approvalService.listPendingAgreements());
    }

    @PostMapping("/{agreementUid}/approve")
    @Operation(summary = "Approve agreement", description = "Approves a pending agreement (maker-checker enforced)")
    public ResponseEntity<Void> approve(
            @PathVariable String agreementUid,
            @RequestBody(required = false) ApproveAgreementRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminUid = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);
        String notes = request != null ? request.getApprovalNotes() : null;

        approvalService.approveAgreement(agreementUid, adminUid, adminRole, notes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{agreementUid}/reject")
    @Operation(summary = "Reject agreement", description = "Rejects a pending agreement with reason (maker-checker enforced)")
    public ResponseEntity<Void> reject(
            @PathVariable String agreementUid,
            @Valid @RequestBody RejectAgreementRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String adminUid = TenantJwt.adminUid(jwt);
        String adminRole = TenantJwt.role(jwt);

        approvalService.rejectAgreement(agreementUid, adminUid, adminRole, request.getRejectionReason());
        return ResponseEntity.ok().build();
    }
}
