package com.loyaltyos.merchants.controller;

import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.merchants.dto.CreateMerchantRequest;
import com.loyaltyos.merchants.dto.MerchantActivateResponse;
import com.loyaltyos.merchants.dto.MerchantApiKeyResponse;
import com.loyaltyos.merchants.dto.MerchantIntegrationTestRequest;
import com.loyaltyos.merchants.dto.MerchantEmailAvailabilityResponse;
import com.loyaltyos.merchants.dto.MerchantInviteLinkResponse;
import com.loyaltyos.merchants.dto.MerchantAgreementPrefillResponse;
import com.loyaltyos.merchants.dto.MerchantAgreementResponse;
import com.loyaltyos.merchants.dto.MerchantResendInviteResponse;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.dto.SubmitMerchantAgreementRequest;
import com.loyaltyos.merchants.dto.UpdateMerchantConfigRequest;
import com.loyaltyos.merchants.dto.UpdateMerchantContactRequest;
import com.loyaltyos.merchants.entity.MerchantOnboardingAudit;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.service.MerchantAgreementService;
import com.loyaltyos.merchants.service.MerchantApiKeyService;
import com.loyaltyos.merchants.service.MerchantCampaignService;
import com.loyaltyos.merchants.service.MerchantIntegrationTestService;
import com.loyaltyos.merchants.dto.MerchantOpsSummaryResponse;
import com.loyaltyos.merchants.dto.MerchantSettlementCycleResponse;
import com.loyaltyos.merchants.dto.SettlementLineItemResponse;
import com.loyaltyos.merchants.service.MerchantConfigApprovalService;
import com.loyaltyos.merchants.service.MerchantExportService;
import com.loyaltyos.merchants.service.MerchantOpsSummaryService;
import com.loyaltyos.merchants.dto.MerchantPendingConfigApprovalResponse;
import com.loyaltyos.merchants.dto.MerchantPendingFinanceAgreementResponse;
import com.loyaltyos.merchants.service.MerchantGovernanceService;
import com.loyaltyos.merchants.service.MerchantSettlementExportService;
import com.loyaltyos.merchants.service.MerchantSettlementService;
import com.loyaltyos.merchants.service.MerchantOnboardingEmailGuard;
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
import java.time.YearMonth;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final MerchantAgreementService merchantAgreementService;
    private final MerchantCampaignService merchantCampaignService;
    private final MerchantIntegrationTestService integrationTestService;
    private final MerchantApiKeyService merchantApiKeyService;
    private final MerchantOnboardingEmailGuard onboardingEmailGuard;
    private final MerchantOpsSummaryService opsSummaryService;
    private final MerchantSettlementService settlementService;
    private final MerchantConfigApprovalService configApprovalService;
    private final MerchantExportService exportService;
    private final MerchantGovernanceService governanceService;
    private final MerchantSettlementExportService settlementExportService;

    public MerchantAdminController(
        MerchantOnboardingService onboardingService,
        MerchantAgreementService merchantAgreementService,
        MerchantCampaignService merchantCampaignService,
        MerchantIntegrationTestService integrationTestService,
        MerchantApiKeyService merchantApiKeyService,
        MerchantOnboardingEmailGuard onboardingEmailGuard,
        MerchantOpsSummaryService opsSummaryService,
        MerchantSettlementService settlementService,
        MerchantConfigApprovalService configApprovalService,
        MerchantExportService exportService,
        MerchantGovernanceService governanceService,
        MerchantSettlementExportService settlementExportService
    ) {
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.merchantAgreementService = Objects.requireNonNull(
            merchantAgreementService, "merchantAgreementService");
        this.merchantCampaignService = Objects.requireNonNull(merchantCampaignService, "merchantCampaignService");
        this.integrationTestService = Objects.requireNonNull(integrationTestService, "integrationTestService");
        this.merchantApiKeyService = Objects.requireNonNull(merchantApiKeyService, "merchantApiKeyService");
        this.onboardingEmailGuard = Objects.requireNonNull(onboardingEmailGuard, "onboardingEmailGuard");
        this.opsSummaryService = Objects.requireNonNull(opsSummaryService, "opsSummaryService");
        this.settlementService = Objects.requireNonNull(settlementService, "settlementService");
        this.configApprovalService = Objects.requireNonNull(configApprovalService, "configApprovalService");
        this.exportService = Objects.requireNonNull(exportService, "exportService");
        this.governanceService = Objects.requireNonNull(governanceService, "governanceService");
        this.settlementExportService = Objects.requireNonNull(settlementExportService, "settlementExportService");
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'merchants.create')")
    public ResponseEntity<MerchantResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateMerchantRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(onboardingService.register(tenantId, request, email));
    }

    @GetMapping("/check-email")
    @PreAuthorize("hasPermission(null, 'merchants.create') or hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantEmailAvailabilityResponse> checkEmail(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam String email,
        @RequestParam(required = false) String excludeMerchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(onboardingEmailGuard.checkPortalEmailAvailability(tenantId, email, excludeMerchantUid));
    }

    @PatchMapping("/{merchantUid}/contact")
    @PreAuthorize(
        "hasPermission(null, 'merchants.edit') or hasPermission(null, 'merchants.approve')"
    )
    public ResponseEntity<MerchantResponse> updateContact(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @Valid @RequestBody UpdateMerchantContactRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(
            onboardingService.updateContactEmail(tenantId, merchantUid, request.getContactEmail(), email));
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

    @GetMapping("/{merchantUid}/agreement/prefill")
    public ResponseEntity<MerchantAgreementPrefillResponse> agreementPrefill(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(merchantAgreementService.getPrefill(tenantId, merchantUid));
    }

    @GetMapping("/{merchantUid}/agreement")
    public ResponseEntity<MerchantAgreementResponse> getAgreement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        MerchantAgreementResponse agreement = merchantAgreementService.getCurrentAgreement(tenantId, merchantUid);
        if (agreement == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(agreement);
    }

    @PutMapping("/{merchantUid}/agreement")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantResponse> submitAgreement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @Valid @RequestBody SubmitMerchantAgreementRequest request
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.submitAgreement(tenantId, merchantUid, request, email));
    }

    @PutMapping("/{merchantUid}/config")
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantActivateResponse> activate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.activate(tenantId, merchantUid, email));
    }

    @PutMapping("/{merchantUid}/resend-invite")
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
    public ResponseEntity<MerchantResendInviteResponse> resendInvite(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.resendPortalInvite(tenantId, merchantUid, email));
    }

    @PostMapping("/{merchantUid}/invite-link")
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
    public ResponseEntity<MerchantInviteLinkResponse> issueInviteLink(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(onboardingService.issuePortalInviteLink(tenantId, merchantUid, email));
    }

    @PutMapping("/{merchantUid}/suspend")
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.edit')")
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
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<CampaignResponse> approveCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(merchantCampaignService.approveMerchantCampaign(tenantId, campaignUid, email));
    }

    @PutMapping("/pending-campaign-approvals/{campaignUid}/reject")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<CampaignResponse> rejectCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(merchantCampaignService.rejectMerchantCampaign(tenantId, campaignUid, email));
    }

    @GetMapping("/export")
    @PreAuthorize("hasPermission(null, 'merchants.export')")
    public ResponseEntity<String> exportMerchants(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merchants-export.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(exportService.exportCsv(tenantId));
    }

    @GetMapping("/{merchantUid}/ops-summary")
    public ResponseEntity<MerchantOpsSummaryResponse> opsSummary(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(opsSummaryService.getOpsSummary(tenantId, merchantUid));
    }

    @GetMapping("/{merchantUid}/campaigns")
    public ResponseEntity<List<CampaignResponse>> listMerchantCampaigns(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(merchantCampaignService.listCampaignsForTenantAdmin(tenantId, merchantUid));
    }

    @PutMapping("/{merchantUid}/agreement/{agreementUid}/approve-finance")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantAgreementResponse> approveFinanceAgreement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String agreementUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(
            merchantAgreementService.approveFinanceAgreement(tenantId, merchantUid, agreementUid, email));
    }

    @PutMapping("/{merchantUid}/config-approvals/{requestUid}/approve")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantResponse> approveConfigRequest(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String requestUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(
            configApprovalService.approveConfigRequest(tenantId, merchantUid, requestUid, email));
    }

    @GetMapping("/{merchantUid}/settlements")
    public ResponseEntity<List<MerchantSettlementCycleResponse>> listSettlements(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(settlementService.listCycles(tenantId, merchantUid));
    }

    @PostMapping("/{merchantUid}/settlements/generate")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantSettlementCycleResponse> generateSettlement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @RequestParam(required = false) String period
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        YearMonth yearMonth = period != null && !period.isBlank()
            ? YearMonth.parse(period)
            : YearMonth.now().minusMonths(1);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(settlementService.generateMonthlyCycle(tenantId, merchantUid, yearMonth));
    }

    @PutMapping("/{merchantUid}/settlements/{cycleUid}/finalize")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<MerchantSettlementCycleResponse> finalizeSettlement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String cycleUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(settlementService.finalizeCycle(tenantId, merchantUid, cycleUid, email));
    }

    @GetMapping("/{merchantUid}/settlements/{cycleUid}/line-items")
    public ResponseEntity<List<SettlementLineItemResponse>> settlementLineItems(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String cycleUid
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(settlementService.listLineItems(tenantId, merchantUid, cycleUid));
    }

    @GetMapping("/pending-finance-agreements")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<List<MerchantPendingFinanceAgreementResponse>> pendingFinanceAgreements(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(governanceService.listPendingFinanceAgreements(tenantId));
    }

    @GetMapping("/pending-config-approvals")
    @PreAuthorize("hasPermission(null, 'merchants.approve')")
    public ResponseEntity<List<MerchantPendingConfigApprovalResponse>> pendingConfigApprovals(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        return ResponseEntity.ok(governanceService.listPendingConfigApprovals(tenantId));
    }

    @GetMapping("/{merchantUid}/settlements/{cycleUid}/export")
    public ResponseEntity<byte[]> exportSettlement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String merchantUid,
        @PathVariable String cycleUid,
        @RequestParam(defaultValue = "csv") String format
    ) {
        String tenantId = TenantJwt.requireTenantAdmin(jwt);
        byte[] body = settlementExportService.export(tenantId, merchantUid, cycleUid, format);
        String ext = format.equalsIgnoreCase("pdf") ? "pdf"
            : (format.equalsIgnoreCase("xlsx") || format.equalsIgnoreCase("excel") ? "xlsx" : "csv");
        String contentType = ext.equals("pdf") ? "application/pdf"
            : (ext.equals("xlsx")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlement-" + cycleUid + "." + ext)
            .contentType(MediaType.parseMediaType(contentType))
            .body(body);
    }
}
