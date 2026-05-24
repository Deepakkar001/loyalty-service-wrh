package com.loyaltyos.integration.controller;

import com.loyaltyos.integration.dto.CredentialSummaryDto;
import com.loyaltyos.integration.dto.DashboardOverviewResponse;
import com.loyaltyos.integration.dto.GenerateCredentialRequest;
import com.loyaltyos.integration.dto.RevealSecretResponse;
import com.loyaltyos.integration.dto.RotateCredentialResponse;
import com.loyaltyos.integration.dto.StatisticsResponse;
import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import com.loyaltyos.integration.service.IntegrationDashboardService;
import com.loyaltyos.onboarding.dto.ApiKeyGeneratedResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@RestController
@Tag(name = "Integration Dashboard", description = "JWT dashboard APIs for integration credentials and audit")
public class IntegrationDashboardController {

    private final IntegrationDashboardService dashboardService;

    public IntegrationDashboardController(IntegrationDashboardService dashboardService) {
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
    }

    @GetMapping("/api/v1/me/integration/dashboard/overview")
    @Operation(summary = "Integration dashboard overview")
    public ResponseEntity<DashboardOverviewResponse> overview(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(dashboardService.getOverview(requireTenantId(jwt)));
    }

    @GetMapping("/api/v1/me/integration/dashboard/credentials")
    @Operation(summary = "List credentials (masked)")
    public ResponseEntity<List<CredentialSummaryDto>> credentials(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "all") String environment
    ) {
        return ResponseEntity.ok(dashboardService.listCredentials(requireTenantId(jwt), environment));
    }

    @PostMapping("/api/v1/me/integration/dashboard/credentials/generate")
    @Operation(summary = "Generate new API credentials")
    public ResponseEntity<ApiKeyGeneratedResponse> generate(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody GenerateCredentialRequest request,
        HttpServletRequest httpRequest
    ) {
        String tenantId = requireTenantId(jwt);
        String userId = jwt.getSubject();
        return ResponseEntity.ok(dashboardService.generateCredentials(tenantId, request, userId, httpRequest));
    }

    @PostMapping("/api/v1/me/integration/dashboard/credentials/{keyId}/reveal-secret")
    @Operation(summary = "Reveal signing secret (logged)")
    public ResponseEntity<RevealSecretResponse> revealSecret(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String keyId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(dashboardService.revealSecret(
            requireTenantId(jwt), keyId, jwt.getSubject(), httpRequest));
    }

    @PostMapping("/api/v1/me/integration/dashboard/credentials/{keyId}/revoke")
    @Operation(summary = "Revoke API credential")
    public ResponseEntity<Void> revoke(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String keyId,
        HttpServletRequest httpRequest
    ) {
        dashboardService.revokeCredential(requireTenantId(jwt), keyId, jwt.getSubject(), httpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/me/integration/dashboard/credentials/{keyId}/rotate")
    @Operation(summary = "Rotate API credential")
    public ResponseEntity<RotateCredentialResponse> rotate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String keyId,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(dashboardService.rotateCredential(
            requireTenantId(jwt), keyId, jwt.getSubject(), httpRequest));
    }

    @GetMapping("/api/v1/me/integration/dashboard/audit-logs")
    @Operation(summary = "Paginated API audit logs")
    public ResponseEntity<Page<ApiRequestAuditLog>> auditLogs(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(dashboardService.getAuditLogs(
            requireTenantId(jwt), PageRequest.of(page, size)));
    }

    @GetMapping("/api/v1/me/integration/dashboard/statistics")
    @Operation(summary = "Usage statistics")
    public ResponseEntity<StatisticsResponse> statistics(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
        return ResponseEntity.ok(dashboardService.getStatistics(requireTenantId(jwt), from, to));
    }

    private static String requireTenantId(Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                "Tenant context is missing from your session. Log in with a tenant account (not platform admin).");
        }
        return tenantId;
    }
}
