package com.loyaltyos.analytics.controller;

import com.loyaltyos.analytics.service.AnalyticsReportExportService;
import com.loyaltyos.analytics.service.ExportService;
import com.loyaltyos.onboarding.security.TenantJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/analytics/export")
@Tag(name = "Analytics Export", description = "Streaming CSV and JSON exports")
@PreAuthorize(
    "hasPermission(null, 'analytics_operational.export') "
        + "or hasPermission(null, 'analytics_finance.export') "
        + "or hasPermission(null, 'analytics_cohort.export')"
)
public class ExportController {

    private final ExportService exportService;
    private final AnalyticsReportExportService analyticsReportExportService;

    public ExportController(ExportService exportService, AnalyticsReportExportService analyticsReportExportService) {
        this.exportService = Objects.requireNonNull(exportService, "exportService");
        this.analyticsReportExportService = Objects.requireNonNull(
            analyticsReportExportService,
            "analyticsReportExportService"
        );
    }

    @GetMapping(value = "/points-ledger", produces = "text/csv")
    @Operation(summary = "Export points ledger as CSV")
    public ResponseEntity<StreamingResponseBody> exportPointsLedger(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "points-ledger-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "id,customer_id,entry_type,points,source_rule_id,created_at\n",
            writer -> exportService.streamPointsLedger(tenantId, programmeUid, from, to, row -> writeLine(writer, row))
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/rule-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Export earn rule configuration bundle")
    public ResponseEntity<Map<String, Object>> exportRuleConfig(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        return ResponseEntity.ok(
            exportService.exportRuleConfigBundle(TenantJwt.tenantId(jwt), programmeUid)
        );
    }

    @GetMapping(value = "/liability-movement", produces = "text/csv")
    @Operation(summary = "Export liability movement report as CSV (BRD §4.17)")
    public ResponseEntity<StreamingResponseBody> exportLiabilityMovement(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "liability-movement-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "programme_uid,month,partial_month,opening_points,opening_monetary,points_issued,points_redeemed,"
                + "points_expired,points_reversed,adjustments_net,net_change_points,net_change_monetary,"
                + "closing_points,closing_monetary\n",
            writer -> exportService.streamLiabilityMovementReport(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/failed-accruals-redemptions", produces = "text/csv")
    @Operation(summary = "Export failed accruals and redemptions operational report as CSV")
    public ResponseEntity<StreamingResponseBody> exportFailedAccrualsRedemptions(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "failed-accruals-redemptions-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "source,transaction_type,programme_uid,customer_id,reference_id,event_type,error_category,"
                + "error_code,error_message,http_status,processing_time_ms,occurred_at\n",
            writer -> exportService.streamFailedAccrualsRedemptionsReport(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/reversals-adjustments", produces = "text/csv")
    @Operation(summary = "Export reversals and adjustments operational report as CSV")
    public ResponseEntity<StreamingResponseBody> exportReversalsAdjustments(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "reversals-adjustments-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "entry_type,ledger_id,programme_uid,customer_id,points,signed_impact,reversal_of_ledger_id,"
                + "original_entry_type,original_points,original_created_at,source_event_id,rule_name,"
                + "description,created_by,created_at\n",
            writer -> exportService.streamReversalsAdjustmentsReport(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/accrual-redemption-reconciliation", produces = "text/csv")
    @Operation(summary = "Export accrual vs redemption reconciliation report as CSV")
    public ResponseEntity<StreamingResponseBody> exportAccrualRedemptionReconciliation(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "accrual-redemption-reconciliation-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,field1,field2,field3,field4,field5,field6,field7,field8,field9,field10,field11,field12,field13,field14,field15\n",
            writer -> analyticsReportExportService.streamAccrualRedemptionReconciliation(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/breakage-expiry", produces = "text/csv")
    @Operation(summary = "Export breakage and expiry report as CSV")
    public ResponseEntity<StreamingResponseBody> exportBreakageExpiry(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "breakage-expiry-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,field1,field2,field3,field4,field5,field6,field7,field8,field9\n",
            writer -> analyticsReportExportService.streamBreakageExpiry(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/enrollment", produces = "text/csv")
    @Operation(summary = "Export enrollment report as CSV")
    public ResponseEntity<StreamingResponseBody> exportEnrollment(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "enrollment-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,field1,field2,field3,field4,field5,field6\n",
            writer -> analyticsReportExportService.streamEnrollment(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/custom-reports", produces = "text/csv")
    @Operation(summary = "Export custom reports bundle (points activity, rule performance, tier distribution) as CSV")
    public ResponseEntity<StreamingResponseBody> exportCustomReports(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "custom-reports-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,field1,field2,field3,field4,field5,field6,field7\n",
            writer -> analyticsReportExportService.streamCustomReports(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/segment-analysis", produces = "text/csv")
    @Operation(summary = "Export segment analysis as CSV")
    public ResponseEntity<StreamingResponseBody> exportSegmentAnalysis(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "segment-analysis-" + programmeUid + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,segment,member_count,avg_balance,total_points_held\n",
            writer -> analyticsReportExportService.streamSegmentAnalysis(
                tenantId,
                programmeUid,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/cohort-retention", produces = "text/csv")
    @Operation(summary = "Export retention cohort grid as CSV")
    public ResponseEntity<StreamingResponseBody> exportCohortRetention(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "cohort-retention-" + programmeUid + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "cohort_month,cohort_size,months_since_join,active_customers,retention_pct\n",
            writer -> analyticsReportExportService.streamCohortRetention(
                tenantId,
                programmeUid,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/cohort-tier-upgrade", produces = "text/csv")
    @Operation(summary = "Export tier upgrade cohort summary as CSV")
    public ResponseEntity<StreamingResponseBody> exportCohortTierUpgrade(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "cohort-tier-upgrade-" + programmeUid + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "cohort_month,cohort_size,reached_silver,silver_pct,avg_days_to_silver,reached_gold,gold_pct,avg_days_to_gold\n",
            writer -> analyticsReportExportService.streamCohortTierUpgrade(
                tenantId,
                programmeUid,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/cohort-tier-velocity", produces = "text/csv")
    @Operation(summary = "Export tier velocity histogram as CSV")
    public ResponseEntity<StreamingResponseBody> exportCohortTierVelocity(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("tierName") String tierName
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "cohort-tier-velocity-" + programmeUid + "-" + tierName + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "tier_name,upgrade_bucket,member_count\n",
            writer -> analyticsReportExportService.streamCohortTierVelocity(
                tenantId,
                programmeUid,
                tierName,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/cohort-rule-effectiveness", produces = "text/csv")
    @Operation(summary = "Export rule effectiveness cohort comparison as CSV")
    public ResponseEntity<StreamingResponseBody> exportCohortRuleEffectiveness(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("ruleUid") String ruleUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "cohort-rule-effectiveness-" + programmeUid + "-" + ruleUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "rule_uid,cohort,member_count,total_points_earned,transaction_count,avg_points_per_member\n",
            writer -> analyticsReportExportService.streamCohortRuleEffectiveness(
                tenantId,
                programmeUid,
                ruleUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/sla-performance", produces = "text/csv")
    @Operation(summary = "Export SLA and performance metrics report as CSV")
    public ResponseEntity<StreamingResponseBody> exportSlaPerformance(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "programmeUid", defaultValue = "default") String programmeUid,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "sla-performance-" + programmeUid + "-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "section,key,label,total_ops,success_ops,success_rate_pct,avg_ms,p50_ms,p95_ms,p99_ms,max_ms,"
                + "sla_success_target_pct,sla_latency_target_ms,sla_status\n",
            writer -> exportService.streamSlaPerformanceReport(
                tenantId,
                programmeUid,
                from,
                to,
                row -> writeLine(writer, row)
            )
        );
        return csvAttachment(filename, body);
    }

    @GetMapping(value = "/webhook-log", produces = "text/csv")
    @Operation(summary = "Export webhook delivery log as CSV")
    public ResponseEntity<StreamingResponseBody> exportWebhookLog(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tenantId = TenantJwt.tenantId(jwt);
        String filename = "webhook-log-" + from + "-to-" + to + ".csv";
        StreamingResponseBody body = out -> streamCsv(
            out,
            "id,event_type,status,attempt_count,last_error,created_at\n",
            writer -> exportService.streamWebhookLog(tenantId, from, to, row -> writeLine(writer, row))
        );
        return csvAttachment(filename, body);
    }

    private static ResponseEntity<StreamingResponseBody> csvAttachment(String filename, StreamingResponseBody body) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body);
    }

    @FunctionalInterface
    private interface CsvStreamAction {
        void run(OutputStreamWriter writer) throws IOException;
    }

    private static void streamCsv(java.io.OutputStream out, String header, CsvStreamAction action) throws IOException {
        var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write(header);
        action.run(writer);
        writer.flush();
    }

    private static void writeLine(OutputStreamWriter writer, String line) {
        try {
            writer.write(line);
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
