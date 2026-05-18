package com.loyaltyos.campaigns.controller;

import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.dto.CampaignParticipationResponse;
import com.loyaltyos.campaigns.service.CampaignAnalyticsService;
import com.loyaltyos.campaigns.service.CampaignService;
import com.loyaltyos.onboarding.security.TenantJwt;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns/admin/campaigns")
@Validated
public class CampaignAdminController {

    private final CampaignService campaignService;
    private final CampaignAnalyticsService analyticsService;

    public CampaignAdminController(CampaignService campaignService, CampaignAnalyticsService analyticsService) {
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CampaignUpsertRequest body
    ) {
        String tenantId = requireTenant(jwt);
        String actor = actorId(jwt, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(tenantId, body, actor));
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponse>> list(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", required = false) String programmeUid,
        @RequestParam(value = "status", required = false) CampaignStatus status
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(campaignService.list(tenantId, programmeUid, status));
    }

    @GetMapping("/{campaignUid}")
    public ResponseEntity<CampaignResponse> get(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(campaignService.get(tenantId, campaignUid));
    }

    @PutMapping("/{campaignUid}")
    public ResponseEntity<CampaignResponse> update(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid,
        @Valid @RequestBody CampaignUpsertRequest body
    ) {
        String tenantId = requireTenant(jwt);
        String actor = actorId(jwt, tenantId);
        return ResponseEntity.ok(campaignService.update(tenantId, campaignUid, body, actor));
    }

    @PostMapping("/{campaignUid}/activate")
    public ResponseEntity<CampaignResponse> activate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(campaignService.activate(tenantId, campaignUid));
    }

    @PostMapping("/{campaignUid}/pause")
    public ResponseEntity<CampaignResponse> pause(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(campaignService.pause(tenantId, campaignUid));
    }

    @PostMapping("/{campaignUid}/end")
    public ResponseEntity<CampaignResponse> end(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(campaignService.end(tenantId, campaignUid));
    }

    @GetMapping("/{campaignUid}/stats")
    public ResponseEntity<CampaignStatsResponse> stats(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(analyticsService.getCampaignStats(tenantId, campaignUid));
    }

    @GetMapping("/{campaignUid}/participations")
    public ResponseEntity<List<CampaignParticipationResponse>> participations(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid,
        @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(analyticsService.listRecentParticipations(tenantId, campaignUid, limit));
    }

    private static String requireTenant(Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Missing tenant");
        }
        return tenantId;
    }

    private static String actorId(Jwt jwt, String tenantId) {
        String email = TenantJwt.email(jwt);
        return email != null && !email.isBlank() ? email : tenantId;
    }
}
