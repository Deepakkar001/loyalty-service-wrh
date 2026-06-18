package com.loyaltyos.merchants.controller;

import com.loyaltyos.campaigns.dto.CampaignParticipationResponse;
import com.loyaltyos.campaigns.dto.CampaignEventSchemaUpsertRequest;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.merchants.dto.MerchantCampaignAnalyticsResponse;
import com.loyaltyos.merchants.dto.MerchantDashboardStatsResponse;
import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.dto.CreateSettlementDisputeRequest;
import com.loyaltyos.merchants.dto.MerchantAgreementResponse;
import com.loyaltyos.merchants.dto.MerchantBudgetAlertResponse;
import com.loyaltyos.merchants.dto.MerchantSettlementCycleResponse;
import com.loyaltyos.merchants.dto.SettlementLineItemResponse;
import com.loyaltyos.merchants.service.MerchantAgreementService;
import com.loyaltyos.merchants.service.MerchantBudgetAlertService;
import com.loyaltyos.merchants.service.MerchantSettlementExportService;
import com.loyaltyos.merchants.service.MerchantSettlementService;
import com.loyaltyos.merchants.service.MerchantCampaignService;
import com.loyaltyos.merchants.service.MerchantDashboardService;
import com.loyaltyos.merchants.service.MerchantOnboardingService;
import com.loyaltyos.merchants.service.MerchantPortalProgrammeService;
import com.loyaltyos.onboarding.dto.ProgrammeConfigBlobResponse;
import com.loyaltyos.onboarding.dto.ProgrammeSummaryResponse;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/merchant")
@Tag(name = "Merchant Portal", description = "Merchant self-service APIs")
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantPortalController {

    private final MerchantOnboardingService onboardingService;
    private final MerchantCampaignService merchantCampaignService;
    private final MerchantDashboardService dashboardService;
    private final MerchantPortalProgrammeService portalProgrammeService;
    private final MerchantAgreementService merchantAgreementService;
    private final MerchantBudgetAlertService budgetAlertService;
    private final MerchantSettlementService settlementService;
    private final MerchantSettlementExportService settlementExportService;

    public MerchantPortalController(
        MerchantOnboardingService onboardingService,
        MerchantCampaignService merchantCampaignService,
        MerchantDashboardService dashboardService,
        MerchantPortalProgrammeService portalProgrammeService,
        MerchantAgreementService merchantAgreementService,
        MerchantBudgetAlertService budgetAlertService,
        MerchantSettlementService settlementService,
        MerchantSettlementExportService settlementExportService
    ) {
        this.onboardingService = Objects.requireNonNull(onboardingService, "onboardingService");
        this.merchantCampaignService = Objects.requireNonNull(merchantCampaignService, "merchantCampaignService");
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService");
        this.portalProgrammeService = Objects.requireNonNull(portalProgrammeService, "portalProgrammeService");
        this.merchantAgreementService = Objects.requireNonNull(merchantAgreementService, "merchantAgreementService");
        this.budgetAlertService = Objects.requireNonNull(budgetAlertService, "budgetAlertService");
        this.settlementService = Objects.requireNonNull(settlementService, "settlementService");
        this.settlementExportService = Objects.requireNonNull(settlementExportService, "settlementExportService");
    }

    @GetMapping("/profile")
    public ResponseEntity<MerchantResponse> profile(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(onboardingService.get(tenantId, merchantUid));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<MerchantDashboardStatsResponse> dashboard(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(dashboardService.getDashboardStats(tenantId, merchantUid));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> listCampaigns(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(merchantCampaignService.listMerchantCampaigns(tenantId, merchantUid));
    }

    @GetMapping("/campaigns/{campaignUid}")
    public ResponseEntity<CampaignResponse> getCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.getMerchantCampaign(tenantId, merchantUid, campaignUid));
    }

    @GetMapping("/campaigns/{campaignUid}/stats")
    public ResponseEntity<CampaignStatsResponse> campaignStats(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.getMerchantCampaignStats(tenantId, merchantUid, campaignUid)
        );
    }

    @PostMapping("/campaigns")
    public ResponseEntity<CampaignResponse> createCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CampaignUpsertRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        String email = TenantJwt.email(jwt);
        CampaignResponse created = merchantCampaignService.createMerchantCampaign(
            tenantId, merchantUid, request, email != null ? email : merchantUid);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/campaigns/{campaignUid}/event-schema")
    public ResponseEntity<CampaignResponse> upsertCampaignEventSchema(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid,
        @Valid @RequestBody CampaignEventSchemaUpsertRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.upsertMerchantCampaignEventSchema(
                tenantId, merchantUid, campaignUid, request)
        );
    }

    @GetMapping("/programmes")
    public ResponseEntity<List<ProgrammeSummaryResponse>> listProgrammes(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        return ResponseEntity.ok(portalProgrammeService.listProgrammes(tenantId));
    }

    @GetMapping("/programmes/{programmeUid}/config")
    public ResponseEntity<ProgrammeConfigBlobResponse> getProgrammeConfig(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String programmeUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        return ResponseEntity.ok(portalProgrammeService.getProgrammeConfig(tenantId, programmeUid));
    }

    @GetMapping("/agreement")
    public ResponseEntity<MerchantAgreementResponse> agreement(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        MerchantAgreementResponse agreement = merchantAgreementService.getCurrentAgreement(tenantId, merchantUid);
        if (agreement == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(agreement);
    }

    @PutMapping("/campaigns/{campaignUid}")
    public ResponseEntity<CampaignResponse> updateCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid,
        @Valid @RequestBody CampaignUpsertRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        String email = TenantJwt.email(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.updateMerchantCampaign(
                tenantId, merchantUid, campaignUid, request, email != null ? email : merchantUid)
        );
    }

    @PutMapping("/campaigns/{campaignUid}/resume")
    public ResponseEntity<CampaignResponse> resumeCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.resumeMerchantCampaign(tenantId, merchantUid, campaignUid));
    }

    @GetMapping("/campaigns/analytics")
    public ResponseEntity<MerchantCampaignAnalyticsResponse> campaignAnalytics(
        @AuthenticationPrincipal Jwt jwt
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(merchantCampaignService.buildMerchantAnalytics(tenantId, merchantUid));
    }

    @PutMapping("/campaigns/{campaignUid}/pause")
    public ResponseEntity<CampaignResponse> pauseCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(merchantCampaignService.pauseMerchantCampaign(tenantId, merchantUid, campaignUid));
    }

    @PutMapping("/campaigns/{campaignUid}/end")
    public ResponseEntity<CampaignResponse> endCampaign(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(merchantCampaignService.endMerchantCampaign(tenantId, merchantUid, campaignUid));
    }

    @GetMapping("/campaigns/{campaignUid}/participations")
    public ResponseEntity<List<CampaignParticipationResponse>> campaignParticipations(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String campaignUid,
        @RequestParam(defaultValue = "50") int limit
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(
            merchantCampaignService.listMerchantCampaignParticipations(tenantId, merchantUid, campaignUid, limit)
        );
    }

    @GetMapping("/budget-alerts")
    public ResponseEntity<List<MerchantBudgetAlertResponse>> budgetAlerts(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(budgetAlertService.listAlertsForMerchant(tenantId, merchantUid));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<MerchantSettlementCycleResponse>> listSettlements(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(settlementService.listCycles(tenantId, merchantUid));
    }

    @GetMapping("/settlements/{cycleUid}")
    public ResponseEntity<MerchantSettlementCycleResponse> getSettlement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String cycleUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(settlementService.getCycle(tenantId, merchantUid, cycleUid));
    }

    @GetMapping("/settlements/{cycleUid}/line-items")
    public ResponseEntity<List<SettlementLineItemResponse>> settlementLineItems(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String cycleUid
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        return ResponseEntity.ok(settlementService.listLineItems(tenantId, merchantUid, cycleUid));
    }

    @PostMapping("/settlements/{cycleUid}/disputes")
    public ResponseEntity<Void> createDispute(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String cycleUid,
        @Valid @RequestBody CreateSettlementDisputeRequest request
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
        settlementService.createDispute(tenantId, merchantUid, cycleUid, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/settlements/{cycleUid}/export")
    public ResponseEntity<byte[]> exportSettlement(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String cycleUid,
        @RequestParam(defaultValue = "csv") String format
    ) {
        String tenantId = TenantJwt.requireTenantId(jwt);
        String merchantUid = TenantJwt.requireMerchantUid(jwt);
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
