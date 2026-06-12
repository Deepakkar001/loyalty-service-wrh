package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.FailedAccrualRedemptionReportResponse;
import com.loyaltyos.analytics.dto.FailedTransactionRow;
import com.loyaltyos.analytics.dto.ReversalAdjustmentLedgerRow;
import com.loyaltyos.analytics.dto.ReversalsAdjustmentsReportResponse;
import com.loyaltyos.analytics.dto.SlaComponentMetricRow;
import com.loyaltyos.analytics.dto.SlaDailyTrendRow;
import com.loyaltyos.analytics.dto.SlaEndpointMetricRow;
import com.loyaltyos.analytics.dto.SlaPerformanceReportResponse;
import com.loyaltyos.analytics.dto.LiabilityMonthlyMovementRow;
import com.loyaltyos.analytics.dto.LiabilityProgrammeRollupRow;
import com.loyaltyos.analytics.dto.LiabilityReportResponse;
import com.loyaltyos.rules.entity.EarnRule;
import com.loyaltyos.rules.repository.EarnRuleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final JdbcTemplate jdbcTemplate;
    private final EarnRuleRepository earnRuleRepository;
    private final LiabilityReportService liabilityReportService;
    private final FailedAccrualRedemptionReportService failedAccrualRedemptionReportService;
    private final ReversalsAdjustmentsReportService reversalsAdjustmentsReportService;
    private final SlaPerformanceReportService slaPerformanceReportService;

    public ExportService(
        JdbcTemplate jdbcTemplate,
        EarnRuleRepository earnRuleRepository,
        LiabilityReportService liabilityReportService,
        FailedAccrualRedemptionReportService failedAccrualRedemptionReportService,
        ReversalsAdjustmentsReportService reversalsAdjustmentsReportService,
        SlaPerformanceReportService slaPerformanceReportService
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.earnRuleRepository = Objects.requireNonNull(earnRuleRepository, "earnRuleRepository");
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

    public void streamPointsLedger(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT id, customer_id, entry_type, points, source_rule_id, created_at
            FROM points_ledger
            WHERE tenant_id = ? AND programme_uid = ? AND created_at >= ? AND created_at < ?
            ORDER BY created_at ASC
            """;
        jdbcTemplate.query(
            sql,
            rs -> {
                String line = String.join(
                    ",",
                    csvCell(rs.getString("id")),
                    csvCell(rs.getString("customer_id")),
                    csvCell(rs.getString("entry_type")),
                    csvCell(rs.getString("points")),
                    csvCell(rs.getString("source_rule_id")),
                    csvCell(rs.getString("created_at"))
                );
                lineConsumer.accept(line);
            },
            tenantId,
            programmeUid,
            fromDt,
            toDt
        );
    }

    public void streamWebhookLog(
        String tenantId,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT wdl.id, wdl.event_type, wdl.status, wdl.attempt_count, wdl.last_error, wdl.created_at
            FROM webhook_delivery_log wdl
            JOIN webhook_subscriptions ws ON ws.id = wdl.subscription_id
            WHERE wdl.tenant_id = ? AND wdl.created_at >= ? AND wdl.created_at < ?
            ORDER BY wdl.created_at ASC
            """;
        jdbcTemplate.query(
            sql,
            rs -> {
                String line = String.join(
                    ",",
                    csvCell(rs.getString("id")),
                    csvCell(rs.getString("event_type")),
                    csvCell(rs.getString("status")),
                    csvCell(rs.getString("attempt_count")),
                    csvCell(rs.getString("last_error")),
                    csvCell(rs.getString("created_at"))
                );
                lineConsumer.accept(line);
            },
            tenantId,
            fromDt,
            toDt
        );
    }

    public void streamLiabilityMovementReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        LiabilityReportResponse report = liabilityReportService.buildReport(tenantId, programmeUid, from, to);
        for (LiabilityMonthlyMovementRow row : report.getMonthlyMovement()) {
            String line = String.join(
                ",",
                csvCell(report.getProgrammeUid()),
                csvCell(row.month()),
                csvCell(Boolean.toString(row.partialMonth())),
                csvCell(decimal(row.openingPoints())),
                csvCell(decimal(row.openingMonetary())),
                csvCell(decimal(row.pointsIssued())),
                csvCell(decimal(row.pointsRedeemed())),
                csvCell(decimal(row.pointsExpired())),
                csvCell(decimal(row.pointsReversed())),
                csvCell(decimal(row.adjustmentsNet())),
                csvCell(decimal(row.netChangePoints())),
                csvCell(decimal(row.netChangeMonetary())),
                csvCell(decimal(row.closingPoints())),
                csvCell(decimal(row.closingMonetary()))
            );
            lineConsumer.accept(line);
        }
        for (LiabilityProgrammeRollupRow rollup : report.getProgrammeRollups()) {
            String line = String.join(
                ",",
                csvCell(rollup.programmeUid()),
                csvCell("ROLLUP"),
                csvCell("false"),
                csvCell(decimal(rollup.outstandingPoints())),
                csvCell(decimal(rollup.outstandingMonetary())),
                csvCell(decimal(rollup.periodPointsIssued())),
                csvCell(decimal(rollup.periodPointsRedeemed())),
                csvCell(decimal(rollup.periodPointsExpired())),
                csvCell(""),
                csvCell(""),
                csvCell(decimal(rollup.periodNetChangePoints())),
                csvCell(decimal(rollup.periodNetChangeMonetary())),
                csvCell(decimal(rollup.outstandingPoints())),
                csvCell(decimal(rollup.outstandingMonetary()))
            );
            lineConsumer.accept(line);
        }
        LiabilityProgrammeRollupRow tenant = report.getTenantRollup();
        if (tenant != null) {
            String line = String.join(
                ",",
                csvCell(tenant.programmeUid()),
                csvCell("ROLLUP"),
                csvCell("false"),
                csvCell(decimal(tenant.outstandingPoints())),
                csvCell(decimal(tenant.outstandingMonetary())),
                csvCell(decimal(tenant.periodPointsIssued())),
                csvCell(decimal(tenant.periodPointsRedeemed())),
                csvCell(decimal(tenant.periodPointsExpired())),
                csvCell(""),
                csvCell(""),
                csvCell(decimal(tenant.periodNetChangePoints())),
                csvCell(decimal(tenant.periodNetChangeMonetary())),
                csvCell(decimal(tenant.outstandingPoints())),
                csvCell(decimal(tenant.outstandingMonetary()))
            );
            lineConsumer.accept(line);
        }
    }

    public void streamFailedAccrualsRedemptionsReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        FailedAccrualRedemptionReportResponse report =
            failedAccrualRedemptionReportService.buildReport(tenantId, programmeUid, from, to);
        for (FailedTransactionRow row : report.getRecentFailures()) {
            String line = String.join(
                ",",
                csvCell(row.getSource()),
                csvCell(row.getTransactionType()),
                csvCell(row.getProgrammeUid()),
                csvCell(row.getCustomerId()),
                csvCell(row.getReferenceId()),
                csvCell(row.getEventType()),
                csvCell(row.getErrorCategory()),
                csvCell(row.getErrorCode()),
                csvCell(row.getErrorMessage()),
                csvCell(row.getHttpStatus() == null ? "" : row.getHttpStatus().toString()),
                csvCell(row.getProcessingTimeMs() == null ? "" : row.getProcessingTimeMs().toString()),
                csvCell(row.getOccurredAt() == null ? "" : row.getOccurredAt().toString())
            );
            lineConsumer.accept(line);
        }
    }

    public void streamSlaPerformanceReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        SlaPerformanceReportResponse report =
            slaPerformanceReportService.buildReport(tenantId, programmeUid, from, to);
        for (SlaComponentMetricRow row : report.getComponents()) {
            lineConsumer.accept(slaComponentCsvLine("COMPONENT", row.componentKey(), row));
        }
        for (SlaEndpointMetricRow row : report.getEndpointBreakdown()) {
            String line = String.join(
                ",",
                csvCell("ENDPOINT"),
                csvCell(row.operationKey()),
                csvCell(row.operationLabel()),
                csvCell(Long.toString(row.requestCount())),
                csvCell(Long.toString(row.successCount())),
                csvCell(decimal(row.successRatePct())),
                csvCell(row.avgLatencyMs() == null ? "" : row.avgLatencyMs().toString()),
                csvCell(row.p99LatencyMs() == null ? "" : row.p99LatencyMs().toString()),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell("")
            );
            lineConsumer.accept(line);
        }
        for (SlaDailyTrendRow row : report.getDailyTrend()) {
            String line = String.join(
                ",",
                csvCell("DAILY"),
                csvCell(row.period()),
                csvCell(""),
                csvCell(Long.toString(row.apiRequests())),
                csvCell(Long.toString(row.apiRequests())),
                csvCell(decimal(row.apiSuccessRatePct())),
                csvCell(row.apiAvgLatencyMs() == null ? "" : row.apiAvgLatencyMs().toString()),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell(""),
                csvCell("")
            );
            lineConsumer.accept(line);
        }
    }

    private static String slaComponentCsvLine(String section, String key, SlaComponentMetricRow row) {
        return String.join(
            ",",
            csvCell(section),
            csvCell(key),
            csvCell(row.componentLabel()),
            csvCell(Long.toString(row.totalOperations())),
            csvCell(Long.toString(row.successfulOperations())),
            csvCell(decimal(row.successRatePct())),
            csvCell(row.avgLatencyMs() == null ? "" : row.avgLatencyMs().toString()),
            csvCell(row.p99LatencyMs() == null ? "" : row.p99LatencyMs().toString()),
            csvCell(row.p50LatencyMs() == null ? "" : row.p50LatencyMs().toString()),
            csvCell(row.p95LatencyMs() == null ? "" : row.p95LatencyMs().toString()),
            csvCell(row.maxLatencyMs() == null ? "" : row.maxLatencyMs().toString()),
            csvCell(decimal(row.slaSuccessRateTargetPct())),
            csvCell(row.slaLatencyTargetMs() == null ? "" : row.slaLatencyTargetMs().toString()),
            csvCell(row.slaStatus())
        );
    }

    public void streamReversalsAdjustmentsReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        ReversalsAdjustmentsReportResponse report =
            reversalsAdjustmentsReportService.buildReport(tenantId, programmeUid, from, to);
        for (ReversalAdjustmentLedgerRow row : report.getReversals()) {
            lineConsumer.accept(reversalAdjustmentCsvLine(row));
        }
        for (ReversalAdjustmentLedgerRow row : report.getAdjustments()) {
            lineConsumer.accept(reversalAdjustmentCsvLine(row));
        }
    }

    private static String reversalAdjustmentCsvLine(ReversalAdjustmentLedgerRow row) {
        return String.join(
            ",",
            csvCell(row.getEntryType()),
            csvCell(row.getLedgerId() == null ? "" : row.getLedgerId().toString()),
            csvCell(row.getProgrammeUid()),
            csvCell(row.getCustomerId()),
            csvCell(decimal(row.getPoints())),
            csvCell(decimal(row.getSignedImpact())),
            csvCell(row.getReversalOfLedgerId() == null ? "" : row.getReversalOfLedgerId().toString()),
            csvCell(row.getOriginalEntryType()),
            csvCell(decimal(row.getOriginalPoints())),
            csvCell(row.getOriginalCreatedAt() == null ? "" : row.getOriginalCreatedAt().toString()),
            csvCell(row.getSourceEventId()),
            csvCell(row.getRuleName()),
            csvCell(row.getDescription()),
            csvCell(row.getCreatedBy()),
            csvCell(row.getCreatedAt() == null ? "" : row.getCreatedAt().toString())
        );
    }

    public Map<String, Object> exportRuleConfigBundle(String tenantId, String programmeUid) {
        List<EarnRule> rules = earnRuleRepository.findByTenantIdAndProgrammeUidOrderByPriorityDesc(tenantId, programmeUid);
        List<Map<String, Object>> rows = rules.stream().map(rule -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ruleUid", rule.getRuleUid());
            m.put("name", rule.getName());
            m.put("status", rule.getStatus() != null ? rule.getStatus().name() : null);
            m.put("triggerEventType", rule.getTriggerEventType());
            m.put("priority", rule.getPriority());
            m.put("effectiveAt", rule.getEffectiveAt());
            m.put("endAt", rule.getEndAt());
            return m;
        }).toList();
        Map<String, Object> bundle = new HashMap<>();
        bundle.put("tenantId", tenantId);
        bundle.put("programmeUid", programmeUid);
        bundle.put("exportedAt", LocalDateTime.now().toString());
        bundle.put("rules", rows);
        return bundle;
    }

    private static String decimal(java.math.BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static String csvCell(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
