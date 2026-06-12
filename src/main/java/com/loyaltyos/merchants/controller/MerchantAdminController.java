package com.loyaltyos.merchants.controller;

import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.merchants.dto.CreateMerchantRequest;
import com.loyaltyos.merchants.dto.MerchantActivateResponse;
import com.loyaltyos.merchants.dto.MerchantApiKeyResponse;
import com.loyaltyos.merchants.dto.MerchantIntegrationTestRequest;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.dto.UpdateMerchantAgreementRequest;
import com.loyaltyos.merchants.dto.UpdateMerchantConfigRequest;
import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.service.MerchantApiKeyService;
import com.loyaltyos.merchants.service.MerchantCampaignService;
import com.loyaltyos.merchants.service.MerchantIntegrationTestService;
import com.loyaltyos.merchants.service.MerchantOnboardingService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/merchants")
@Tag(name = "Merchant Admin", description = "Tenant-scoped merchant onboarding")
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantAdminController {

    private final MerchantOnboardingService onboardingService;
    private final MerchantCampaignService merchantCampaignService;
    private final MerchantIntegrationTestService integrationTestService;
    private final MerchantApiKeyService merchantApiKeyService;

    public MerchantAdminController(
        MerchantOnboardingService onboardingService,
        MerchantCampaignService merchantCampaignService,
        MerchantIntegrationTestService integrationTestService,
        MerchantApiKeyService merchantApiKeyService
    ) {
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.merchantCampaignService = Objects.requireNonNull(merchantCampaignService, "merchantCampaignService");
        this.integrationTestService = Objects.requireNonNull(integrationTestService, "integrationTestService");
        this.merchantApiKeyService = Objects.requireNonNull(merchantApiKeyService, "merchantApiKeyService");
    }

    @PostMapping
    @PreAuthorize("hasPermission('merchants.create')")
    public ResponseEntity<MerchantResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateMerchantRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(onboardingService.register(tenantId, request, email));
    }

    @GetMapping
    public ResponseEntity<Page<MerchantResponse>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String stage
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        MerchantOnboardingStage filter = stage != null && !stage.isBlank()
            ? MerchantOnboardingStage.valueOf(stage.trim().toUpperCase())
            : null;
        Page<MerchantResponse> result = onboardingService.list(
            tenantId,
            filter,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{merchantUid}")
    public ResponseEntity<MerchantResponse> get(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(onboardingService.get(tenantId, merchantUid));
    }

    @GetMapping("/{merchantUid}/audit")
    public ResponseEntity<List<MerchantOnboardingAudit>> audit(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(onboardingService.auditTrail(tenantId, merchantUid));
    }

    @PutMapping("/{merchantUid}/agreement")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantResponse> submitAgreement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @Valid @RequestBody UpdateMerchantAgreementRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.submitAgreement(tenantId, merchantUid, request, email));
    }

    @PutMapping("/{merchantUid}/config")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantResponse> configure(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @Valid @RequestBody UpdateMerchantConfigRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.configure(tenantId, merchantUid, request, email));
    }

    @PostMapping("/{merchantUid}/integration")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantResponse> completeIntegration(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @Valid @RequestBody MerchantIntegrationTestRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(integrationTestService.runSandboxTest(tenantId, merchantUid, request, email));
    }

    @GetMapping("/{merchantUid}/api-keys")
    public ResponseEntity<List<MerchantApiKeyResponse>> listApiKeys(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(merchantApiKeyService.listKeys(tenantId, merchantUid));
    }

    @PostMapping("/{merchantUid}/api-keys")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantApiKeyResponse> createApiKey(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String name = body != null ? body.get("name") : null;
        String environment = body != null ? body.get("environment") : null;
        MerchantApiKeyResponse created = merchantApiKeyService.createKey(tenantId, merchantUid, name, environment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{merchantUid}/api-keys/{keyUid}/revoke")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<Void> revokeApiKey(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String keyUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        merchantApiKeyService.revokeKey(tenantId, merchantUid, keyUid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{merchantUid}/activate")
    @PreAuthorize("hasPermission('merchants.approve')")
    public ResponseEntity<MerchantActivateResponse> activate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.activate(tenantId, merchantUid, email));
    }

    @PutMapping("/{merchantUid}/suspend")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantResponse> suspend(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        String reason = body != null ? body.getOrDefault("reason", "Suspended by admin") : "Suspended by admin";
        return ResponseEntity.ok(onboardingService.suspend(tenantId, merchantUid, reason, email));
    }

    @PutMapping("/{merchantUid}/unsuspend")
    @PreAuthorize("hasPermission('merchants.edit')")
    public ResponseEntity<MerchantResponse> unsuspend(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.unsuspend(tenantId, merchantUid, email));
    }

    @GetMapping("/pending-campaign-approvals")
    public ResponseEntity<List<CampaignResponse>> pendingCampaignApprovals(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(merchantCampaignService.listPendingApprovals(tenantId));
    }

    @PutMapping("/pending-campaign-approvals/{campaignUid}/approve")
    @PreAuthorize("hasPermission('merchants.approve')")
    public ResponseEntity<CampaignResponse> approveCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(merchantCampaignService.approveMerchantCampaign(tenantId, campaignUid, email));
    }

    @PutMapping("/pending-campaign-approvals/{campaignUid}/reject")
    @PreAuthorize("hasPermission('merchants.approve')")
    public ResponseEntity<CampaignResponse> rejectCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(merchantCampaignService.rejectMerchantCampaign(tenantId, campaignUid, email));
    }
}
