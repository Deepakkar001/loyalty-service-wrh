package com.loyaltyos.onboarding.controller;

import com.loyaltyos.onboarding.domain.enums.ApiKeyEnvironment;
import com.loyaltyos.onboarding.dto.request.SandboxValidateEventRequest;
import com.loyaltyos.onboarding.dto.response.ApiKeyGeneratedResponse;
import com.loyaltyos.onboarding.dto.response.ApiKeySummaryResponse;
import com.loyaltyos.onboarding.dto.response.WebhookStatusResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.onboarding.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Integration", description = "Stage 5: API keys, webhook verification, sandbox validation")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = Objects.requireNonNull(integrationService, "integrationService");
    }

    // ─── Legacy endpoint used by existing frontend ─────────────────────────────

    @PostMapping("/api/v1/onboarding/{tenantId}/keys/sandbox")
    @Operation(summary = "Legacy: Generate sandbox API keys",
        description = "Required by current frontend onboardingApi.generateSandboxKeys().")
    public ResponseEntity<ApiKeyGeneratedResponse> generateSandboxKeysLegacy(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("tenantId") String tenantId
    ) {
        String jwtTenantId = TenantJwt.tenantId(jwt);
        if (jwtTenantId == null || !jwtTenantId.equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(integrationService.generateSandboxKeysLegacy(tenantId));
    }

    // ─── New tenant-scoped endpoints (dashboard integrate page) ────────────────

    @PostMapping("/api/v1/me/integration/credentials")
    @Operation(summary = "Generate API key credentials (sandbox + production)")
    public ResponseEntity<ApiKeyGeneratedResponse> generateCredentials(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        // Default to SANDBOX for safer initial onboarding; production can be generated explicitly.
        return ResponseEntity.ok(integrationService.generateKeys(tenantId, ApiKeyEnvironment.SANDBOX));
    }

    @PostMapping("/api/v1/me/integration/credentials/{environment}")
    @Operation(summary = "Generate API key credentials for an environment")
    public ResponseEntity<ApiKeyGeneratedResponse> generateCredentialsForEnv(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("environment") ApiKeyEnvironment environment
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(integrationService.generateKeys(tenantId, environment));
    }

    @GetMapping("/api/v1/me/integration/credentials")
    @Operation(summary = "Get credentials summary (prefix only)")
    public ResponseEntity<List<ApiKeySummaryResponse>> getCredentialSummaries(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(integrationService.getKeySummaries(tenantId));
    }

    @PostMapping("/api/v1/me/integration/webhook/verify")
    @Operation(summary = "Verify webhook endpoint")
    public ResponseEntity<WebhookStatusResponse> verifyWebhook(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(integrationService.verifyWebhook(tenantId));
    }

    @GetMapping("/api/v1/me/integration/webhook/status")
    @Operation(summary = "Get webhook verification status")
    public ResponseEntity<WebhookStatusResponse> webhookStatus(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(integrationService.getWebhookStatus(tenantId));
    }

    @PostMapping("/api/v1/me/integration/sandbox/validate-event")
    @Operation(summary = "Sandbox: validate event payload JSON")
    public ResponseEntity<Map<String, Object>> validateSandboxEvent(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody SandboxValidateEventRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(integrationService.validateSandboxEvent(tenantId, request));
    }
}

