package com.loyaltyos.onboarding.analytics.controller;

import com.loyaltyos.onboarding.analytics.dto.CohortRetentionRow;
import com.loyaltyos.onboarding.analytics.dto.PointsActivityRow;
import com.loyaltyos.onboarding.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.onboarding.analytics.dto.RulePerformanceRow;
import com.loyaltyos.onboarding.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.onboarding.analytics.dto.TierDistributionRow;
import com.loyaltyos.onboarding.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.onboarding.analytics.dto.TierVelocityBucketRow;
import com.loyaltyos.onboarding.analytics.service.AnalyticsService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Tenant analytics and reporting")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    @GetMapping("/points-activity")
    @Operation(summary = "Points activity report")
    public ResponseEntity<List<PointsActivityRow>> pointsActivity(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            analyticsService.getPointsActivity(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
    }

    @GetMapping("/rule-performance")
    @Operation(summary = "Rule performance report")
    public ResponseEntity<List<RulePerformanceRow>> rulePerformance(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            analyticsService.getRulePerformance(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
    }

    @GetMapping("/tier-distribution")
    @Operation(summary = "Tier distribution snapshot")
    public ResponseEntity<List<TierDistributionRow>> tierDistribution(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            analyticsService.getTierDistribution(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping("/segments/engagement")
    @Operation(summary = "Engagement segment breakdown")
    public ResponseEntity<List<SegmentAnalysisRow>> engagementSegments(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            analyticsService.getEngagementSegments(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping("/segments/balance-brackets")
    @Operation(summary = "Balance bracket segment breakdown")
    public ResponseEntity<List<SegmentAnalysisRow>> balanceBrackets(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            analyticsService.getBalanceBrackets(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping("/cohorts/retention")
    @Operation(summary = "Monthly retention cohort grid")
    public ResponseEntity<List<CohortRetentionRow>> retentionCohort(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            analyticsService.getRetentionCohort(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping("/cohorts/tier-upgrade")
    @Operation(summary = "Tier upgrade cohort summary")
    public ResponseEntity<List<TierUpgradeCohortRow>> tierUpgradeCohort(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            analyticsService.getTierUpgradeCohort(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping("/cohorts/tier-velocity")
    @Operation(summary = "Days-to-tier histogram buckets")
    public ResponseEntity<List<TierVelocityBucketRow>> tierVelocity(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("tierName") String tierName
    ) {
        return ResponseEntity.ok(
            analyticsService.getTierVelocityBuckets(TenantJwt.tenantId(jwt), programmeUid, tierName)
        );
    }

    @GetMapping("/cohorts/rule-effectiveness")
    @Operation(summary = "Rule exposure vs non-exposure cohort")
    public ResponseEntity<List<RuleEffectivenessRow>> ruleEffectiveness(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("ruleUid") String ruleUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            analyticsService.getRuleEffectiveness(TenantJwt.tenantId(jwt), programmeUid, ruleUid, from, to)
        );
    }
}
