package com.loyaltyos.campaigns.controller;

import com.loyaltyos.campaigns.dto.CampaignPerformanceReportResponse;
import com.loyaltyos.campaigns.service.CampaignAnalyticsService;
import com.loyaltyos.onboarding.security.TenantJwt;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns/admin/reports")
@Validated
public class CampaignReportsController {

    private final CampaignAnalyticsService analyticsService;

    public CampaignReportsController(CampaignAnalyticsService analyticsService) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    @GetMapping("/performance")
    public ResponseEntity<CampaignPerformanceReportResponse> performanceReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("programmeUid") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(analyticsService.buildPerformanceReport(tenantId, programmeUid, from, to));
    }

    private static String requireTenant(Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Missing tenant");
        }
        return tenantId;
    }
}
