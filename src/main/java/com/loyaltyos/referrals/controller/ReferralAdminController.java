package com.loyaltyos.referrals.controller;

import com.loyaltyos.onboarding.security.TenantJwt;
import com.loyaltyos.referrals.dto.ReferralDashboardResponse;
import com.loyaltyos.referrals.dto.ReferralFraudQueueItemResponse;
import com.loyaltyos.referrals.dto.ReferralFraudReviewRequest;
import com.loyaltyos.referrals.dto.ReferralListItemResponse;
import com.loyaltyos.referrals.dto.ReferralProgrammeUpsertRequest;
import com.loyaltyos.referrals.dto.ReferralRuleSchemaResponse;
import com.loyaltyos.referrals.support.ReferralMilestoneRuleRegistry;
import com.loyaltyos.referrals.support.ReferralProgrammeSchemaLoader;
import com.loyaltyos.referrals.support.ReferralRuleSchemaSupport;
import com.loyaltyos.referrals.dto.ReferralEffectivenessReportResponse;
import com.loyaltyos.referrals.dto.ReferralTimeToPurchaseResponse;
import com.loyaltyos.referrals.dto.ReferralTopReferrerResponse;
import com.loyaltyos.referrals.dto.ReferralTrendPointResponse;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.service.ReferralAdminService;
import com.loyaltyos.referrals.service.ReferralAnalyticsService;
import com.loyaltyos.referrals.service.ReferralFraudReviewService;
import com.loyaltyos.referrals.service.ReferralProgrammeService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/referrals")
public class ReferralAdminController {

    private final ReferralProgrammeService programmeService;
    private final ReferralAdminService adminService;
    private final ReferralFraudReviewService fraudReviewService;
    private final ReferralAnalyticsService analyticsService;
    private final ReferralProgrammeSchemaLoader programmeSchemaLoader;

    public ReferralAdminController(
        ReferralProgrammeService programmeService,
        ReferralAdminService adminService,
        ReferralFraudReviewService fraudReviewService,
        ReferralAnalyticsService analyticsService,
        ReferralProgrammeSchemaLoader programmeSchemaLoader
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.adminService = Objects.requireNonNull(adminService, "adminService");
        this.fraudReviewService = Objects.requireNonNull(fraudReviewService, "fraudReviewService");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
        this.programmeSchemaLoader = Objects.requireNonNull(programmeSchemaLoader, "programmeSchemaLoader");
    }

    @PostMapping("/programmes")
    @PreAuthorize("hasPermission(null, 'referrals.create')")
    public ResponseEntity<Map<String, Object>> upsertProgramme(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ReferralProgrammeUpsertRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        ReferralProgramme saved = programmeService.upsert(tenantId, request);
        Map<String, Object> body = new HashMap<>();
        body.put("programmeUid", saved.getProgrammeUid());
        body.put("name", saved.getName());
        body.put("status", saved.getStatus().name());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/rule-schema")
    public ResponseEntity<ReferralRuleSchemaResponse> ruleSchema(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String programmeUid
    ) {
        if (programmeUid == null || programmeUid.isBlank()) {
            return ResponseEntity.ok(ReferralRuleSchemaSupport.build());
        }
        String tenantId = TenantJwt.tenantId(jwt);
        String uid = programmeUid.trim();
        return ResponseEntity.ok(
            ReferralRuleSchemaSupport.buildForProgramme(uid, programmeSchemaLoader.loadForProgramme(tenantId, uid))
        );
    }

    @GetMapping("/programmes")
    public ResponseEntity<Map<String, Object>> getProgramme(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return programmeService.find(tenantId, programmeUid)
            .map(programme -> {
                ReferralProgrammeConfig config = adminService.readConfig(programme);
                Map<String, Object> body = new HashMap<>();
                body.put("programmeUid", programme.getProgrammeUid());
                body.put("name", programme.getName());
                body.put("status", programme.getStatus().name());
                body.put("validFrom", programme.getValidFrom());
                body.put("validUntil", programme.getValidUntil());
                body.put("maxReferralsPerCustomer", programme.getMaxReferralsPerCustomer());
                body.put("config", config);
                body.put("milestoneTypes", ReferralMilestoneRuleRegistry.toDisplayList(config));
                return ResponseEntity.ok(body);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ReferralDashboardResponse> dashboard(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (fromDate != null || toDate != null) {
            validateDateRange(fromDate, toDate);
            return ResponseEntity.ok(analyticsService.dashboardForPeriod(tenantId, programmeUid, fromDate, toDate));
        }
        return ResponseEntity.ok(adminService.dashboard(tenantId, programmeUid));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReferralListItemResponse>> listReferrals(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(required = false) String status
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(adminService.listReferrals(tenantId, programmeUid, status));
    }

    @GetMapping("/fraud-queue")
    public ResponseEntity<List<ReferralFraudQueueItemResponse>> fraudQueue(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(fraudReviewService.listFraudQueue(tenantId, programmeUid));
    }

    @PostMapping("/fraud-queue/{referralUid}/approve")
    @PreAuthorize("hasPermission(null, 'referrals.approve')")
    public ResponseEntity<Map<String, String>> approveFraud(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String referralUid,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestBody(required = false) ReferralFraudReviewRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        fraudReviewService.approve(tenantId, programmeUid, referralUid, request != null ? request.getNote() : null);
        return ResponseEntity.ok(Map.of("status", "APPROVED", "referralUid", referralUid));
    }

    @PostMapping("/fraud-queue/{referralUid}/reject")
    @PreAuthorize("hasPermission(null, 'referrals.approve')")
    public ResponseEntity<Map<String, String>> rejectFraud(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String referralUid,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestBody(required = false) ReferralFraudReviewRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        fraudReviewService.reject(tenantId, programmeUid, referralUid, request != null ? request.getNote() : null);
        return ResponseEntity.ok(Map.of("status", "REJECTED", "referralUid", referralUid));
    }

    @PostMapping("/fraud-queue/{referralUid}/override")
    @PreAuthorize("hasPermission(null, 'referrals.approve')")
    public ResponseEntity<Map<String, String>> overrideFraud(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String referralUid,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestBody(required = false) ReferralFraudReviewRequest request
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        fraudReviewService.override(tenantId, programmeUid, referralUid, request != null ? request.getNote() : null);
        return ResponseEntity.ok(Map.of("status", "OVERRIDE", "referralUid", referralUid));
    }

    @GetMapping("/analytics/trends")
    public ResponseEntity<List<ReferralTrendPointResponse>> trends(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(defaultValue = "DAILY") String granularity,
        @RequestParam(defaultValue = "30") int days,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (fromDate != null || toDate != null) {
            validateDateRange(fromDate, toDate);
            return ResponseEntity.ok(analyticsService.trendsForPeriod(tenantId, programmeUid, fromDate, toDate));
        }
        return ResponseEntity.ok(analyticsService.trends(tenantId, programmeUid, granularity, days));
    }

    @GetMapping("/analytics/top-referrers")
    public ResponseEntity<List<ReferralTopReferrerResponse>> topReferrers(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (fromDate != null || toDate != null) {
            validateDateRange(fromDate, toDate);
            return ResponseEntity.ok(
                analyticsService.topReferrersForPeriod(tenantId, programmeUid, fromDate, toDate, limit)
            );
        }
        return ResponseEntity.ok(analyticsService.topReferrers(tenantId, programmeUid, limit));
    }

    @GetMapping("/analytics/time-to-first-purchase")
    public ResponseEntity<ReferralTimeToPurchaseResponse> timeToFirstPurchase(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(analyticsService.timeToFirstPurchase(tenantId, programmeUid));
    }

    @GetMapping("/analytics/effectiveness-report")
    public ResponseEntity<ReferralEffectivenessReportResponse> effectivenessReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        return ResponseEntity.ok(analyticsService.buildEffectivenessReport(tenantId, programmeUid, from, to));
    }

    @GetMapping(value = "/analytics/effectiveness-report/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportEffectivenessReport(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        StringBuilder csv = new StringBuilder(
            "section,field1,field2,field3,field4,field5,field6,field7\n"
        );
        analyticsService.streamEffectivenessReportCsv(tenantId, programmeUid, from, to, line -> {
            csv.append(line).append('\n');
        });
        String filename = "referral-effectiveness-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportReferrals(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "default") String programmeUid,
        @RequestParam(required = false) String status
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String csv = adminService.exportReferralsCsv(tenantId, programmeUid, status);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=referrals-export.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(bytes);
    }

    private static void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate must both be provided");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > 366) {
            throw new IllegalArgumentException("Date range cannot exceed 366 days");
        }
        if (toDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("toDate cannot be in the future");
        }
    }
}
