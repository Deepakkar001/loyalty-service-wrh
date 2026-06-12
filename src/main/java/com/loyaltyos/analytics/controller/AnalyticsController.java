package com.loyaltyos.analytics.controller;

import com.loyaltyos.analytics.dto.CohortRetentionRow;
import com.loyaltyos.analytics.dto.PointsActivityRow;
import com.loyaltyos.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.analytics.dto.RulePerformanceRow;
import com.loyaltyos.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.analytics.dto.TierDistributionRow;
import com.loyaltyos.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.analytics.dto.TierVelocityBucketRow;
import com.loyaltyos.analytics.dto.AccrualRedemptionReconciliationResponse;
import com.loyaltyos.analytics.dto.BreakageExpiryReportResponse;
import com.loyaltyos.analytics.dto.EnrollmentReportResponse;
import com.loyaltyos.analytics.dto.FailedAccrualRedemptionReportResponse;
import com.loyaltyos.analytics.dto.LiabilityReportResponse;
import com.loyaltyos.analytics.service.AccrualRedemptionReconciliationService;
import com.loyaltyos.analytics.service.AnalyticsService;
import com.loyaltyos.analytics.service.BreakageExpiryReportService;
import com.loyaltyos.analytics.service.EnrollmentReportService;
import com.loyaltyos.analytics.dto.ReversalsAdjustmentsReportResponse;
import com.loyaltyos.analytics.dto.SlaPerformanceReportResponse;
import com.loyaltyos.analytics.service.FailedAccrualRedemptionReportService;
import com.loyaltyos.analytics.service.LiabilityReportService;
import com.loyaltyos.analytics.service.ReversalsAdjustmentsReportService;
import com.loyaltyos.analytics.service.SlaPerformanceReportService;
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
    private final BreakageExpiryReportService breakageExpiryReportService;
    private final EnrollmentReportService enrollmentReportService;
    private final AccrualRedemptionReconciliationService accrualRedemptionReconciliationService;
    private final LiabilityReportService liabilityReportService;
    private final FailedAccrualRedemptionReportService failedAccrualRedemptionReportService;
    private final ReversalsAdjustmentsReportService reversalsAdjustmentsReportService;
    private final SlaPerformanceReportService slaPerformanceReportService;

    public AnalyticsController(
        AnalyticsService analyticsService,
        BreakageExpiryReportService breakageExpiryReportService,
        EnrollmentReportService enrollmentReportService,
        AccrualRedemptionReconciliationService accrualRedemptionReconciliationService,
        LiabilityReportService liabilityReportService,
        FailedAccrualRedemptionReportService failedAccrualRedemptionReportService,
        ReversalsAdjustmentsReportService reversalsAdjustmentsReportService,
        SlaPerformanceReportService slaPerformanceReportService
    ) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
        this.breakageExpiryReportService = Objects.requireNonNull(
            breakageExpiryReportService,
            "breakageExpiryReportService"
        );
        this.enrollmentReportService = Objects.requireNonNull(
            enrollmentReportService,
            "enrollmentReportService"
        );
        this.accrualRedemptionReconciliationService = Objects.requireNonNull(
            accrualRedemptionReconciliationService,
            "accrualRedemptionReconciliationService"
        );
        this.liabilityReportService = Objects.requireNonNull(liabilityReportService, "liabilityReportService");
        this.failedAccrualRedemptionReportService = Objects.requireNonNull(
            failedAccrualRedemptionReportService,
            "failedAccrualRedemptionReportService"
        );
        this.reversalsAdjustmentsReportService = Objects.requireNonNull(
            reversalsAdjustmentsReportService,
            "reversalsAdjustmentsReportService"
        );
        this.slaPerformanceReportService = Objects.requireNonNull(
            slaPerformanceReportService,
            "slaPerformanceReportService"
        );
    }

    @GetMapping("/reports/enrollment")
    @Operation(summary = "Member enrollment and acquisition report")
    public ResponseEntity<EnrollmentReportResponse> enrollmentReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            enrollmentReportService.buildReport(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
    }

    @GetMapping("/reports/accrual-redemption-reconciliation")
    @Operation(summary = "Accrual vs redemption liability reconciliation report")
    public ResponseEntity<AccrualRedemptionReconciliationResponse> accrualRedemptionReconciliation(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            accrualRedemptionReconciliationService.buildReport(
                TenantJwt.tenantId(jwt),
                programmeUid,
                from,
                to
            )
        );
    }

    @GetMapping("/reports/liability")
    @Operation(summary = "Loyalty liability movement and roll-up report")
    public ResponseEntity<LiabilityReportResponse> liabilityReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            liabilityReportService.buildReport(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
    }

    @GetMapping("/reports/failed-accruals-redemptions")
    @Operation(summary = "Failed accruals and redemptions operational report")
    public ResponseEntity<FailedAccrualRedemptionReportResponse> failedAccrualsRedemptionsReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            failedAccrualRedemptionReportService.buildReport(
                TenantJwt.tenantId(jwt),
                programmeUid,
                from,
                to
            )
        );
    }

    @GetMapping("/reports/sla-performance")
    @Operation(summary = "Loyalty-engine SLA and performance metrics report")
    public ResponseEntity<SlaPerformanceReportResponse> slaPerformanceReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            slaPerformanceReportService.buildReport(
                TenantJwt.tenantId(jwt),
                programmeUid,
                from,
                to
            )
        );
    }

    @GetMapping("/reports/reversals-adjustments")
    @Operation(summary = "Reversals and adjustments operational report")
    public ResponseEntity<ReversalsAdjustmentsReportResponse> reversalsAdjustmentsReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            reversalsAdjustmentsReportService.buildReport(
                TenantJwt.tenantId(jwt),
                programmeUid,
                from,
                to
            )
        );
    }

    @GetMapping("/reports/breakage-expiry")
    @Operation(summary = "Breakage and point expiry finance report")
    public ResponseEntity<BreakageExpiryReportResponse> breakageExpiryReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            breakageExpiryReportService.buildReport(TenantJwt.tenantId(jwt), programmeUid, from, to)
        );
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
