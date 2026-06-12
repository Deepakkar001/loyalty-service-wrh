package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.SlaComponentMetricRow;
import com.loyaltyos.analytics.dto.SlaDailyTrendRow;
import com.loyaltyos.analytics.dto.SlaEndpointMetricRow;
import com.loyaltyos.analytics.dto.SlaPerformanceReportResponse;
import com.loyaltyos.analytics.repository.SlaPerformanceQueryRepository;
import com.loyaltyos.analytics.repository.SlaPerformanceQueryRepository.DailyAggregate;
import com.loyaltyos.analytics.repository.SlaPerformanceQueryRepository.OperationAggregate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SlaPerformanceReportService {

    private static final String REPORT_DEFINITION =
        "Loyalty-engine SLA and performance report (BRD §6.3): success rates and latency percentiles "
            + "for integration APIs (api_request_audit_log), reward issuance (reward_issuance_audit, programme-scoped), "
            + "and async event processing (integration_event_processing_log, tenant-wide). "
            + "Default SLA targets are platform baselines; compare daily trends and endpoint breakdown for ops triage.";

    private static final BigDecimal API_SUCCESS_TARGET = new BigDecimal("99.0");
    private static final BigDecimal ISSUANCE_SUCCESS_TARGET = new BigDecimal("99.5");
    private static final BigDecimal EVENT_SUCCESS_TARGET = new BigDecimal("99.0");

    private static final int API_P99_TARGET_MS = 500;
    private static final int ISSUANCE_P99_TARGET_MS = 1000;
    private static final int EVENT_P99_TARGET_MS = 2000;

    private static final Map<String, String> ENDPOINT_LABELS = Map.of(
        "EVENT_PROCESS", "POST /events/process",
        "REDEMPTION", "POST /redemptions",
        "REDEMPTION_VALIDATE", "POST /redemptions/validate",
        "BALANCE", "GET /balance",
        "EVENT_VALIDATE", "POST /events/validate",
        "OTHER", "Other integration APIs"
    );

    private final SlaPerformanceQueryRepository queryRepository;

    public SlaPerformanceReportService(SlaPerformanceQueryRepository queryRepository) {
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository");
    }

    public SlaPerformanceReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        OperationAggregate apiAgg = queryRepository.getApiAggregate(tenantId, from, to, "%");
        List<Integer> apiLatencies = queryRepository.getApiLatencies(tenantId, from, to, "%");
        OperationAggregate priorApiAgg = queryRepository.getApiAggregate(tenantId, priorFrom, priorTo, "%");

        OperationAggregate issuanceAgg = queryRepository.getIssuanceAggregate(tenantId, programmeUid, from, to);
        List<Integer> issuanceLatencies = queryRepository.getIssuanceLatencies(
            tenantId, programmeUid, from, to, "SUCCESS"
        );

        OperationAggregate eventAgg = queryRepository.getEventProcessingAggregate(tenantId, from, to, false);
        List<Integer> eventLatencies = queryRepository.getEventProcessingLatencies(tenantId, from, to);

        BigDecimal apiSuccessRate = ratePct(apiAgg.successOps(), apiAgg.totalOps());
        BigDecimal priorApiSuccessRate = ratePct(priorApiAgg.successOps(), priorApiAgg.totalOps());
        BigDecimal issuanceSuccessRate = ratePct(issuanceAgg.successOps(), issuanceAgg.totalOps());
        BigDecimal eventSuccessRate = ratePct(eventAgg.successOps(), eventAgg.totalOps());

        int apiP99 = percentile(apiLatencies, 99);
        int issuanceP99 = percentile(issuanceLatencies, 99);
        int eventP99 = percentile(eventLatencies, 99);

        List<SlaComponentMetricRow> components = buildComponents(
            apiAgg,
            apiLatencies,
            issuanceAgg,
            issuanceLatencies,
            eventAgg,
            eventLatencies
        );

        SlaPerformanceReportResponse report = new SlaPerformanceReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setReportDefinition(REPORT_DEFINITION);

        report.setTotalApiRequests(apiAgg.totalOps());
        report.setOverallApiSuccessRatePct(apiSuccessRate);
        report.setOverallApiP99LatencyMs(apiP99);
        report.setTotalIssuanceAttempts(issuanceAgg.totalOps());
        report.setIssuanceSuccessRatePct(issuanceSuccessRate);
        report.setIssuanceP99LatencyMs(issuanceP99);
        report.setTotalEventProcessingAttempts(eventAgg.totalOps());
        report.setEventProcessingSuccessRatePct(eventSuccessRate);
        report.setEventProcessingP99LatencyMs(eventP99);

        report.setPriorPeriodApiSuccessRatePct(priorApiSuccessRate);
        report.setPeriodOverPeriodApiSuccessChangePct(percentChange(apiSuccessRate, priorApiSuccessRate));

        report.setComponents(components);
        report.setEndpointBreakdown(buildEndpointBreakdown(tenantId, from, to));
        report.setDailyTrend(buildDailyTrend(tenantId, programmeUid, from, to));
        report.setOverallSlaStatus(overallStatus(components));
        return report;
    }

    private List<SlaComponentMetricRow> buildComponents(
        OperationAggregate apiAgg,
        List<Integer> apiLatencies,
        OperationAggregate issuanceAgg,
        List<Integer> issuanceLatencies,
        OperationAggregate eventAgg,
        List<Integer> eventLatencies
    ) {
        List<SlaComponentMetricRow> rows = new ArrayList<>();
        rows.add(componentRow(
            "INTEGRATION_API",
            "Integration APIs (tenant-wide)",
            apiAgg,
            apiLatencies,
            API_SUCCESS_TARGET,
            API_P99_TARGET_MS
        ));
        rows.add(componentRow(
            "REWARD_ISSUANCE",
            "Reward issuance engine (programme)",
            issuanceAgg,
            issuanceLatencies,
            ISSUANCE_SUCCESS_TARGET,
            ISSUANCE_P99_TARGET_MS
        ));
        rows.add(componentRow(
            "EVENT_PROCESSING",
            "Async event processing (tenant-wide)",
            eventAgg,
            eventLatencies,
            EVENT_SUCCESS_TARGET,
            EVENT_P99_TARGET_MS
        ));
        return rows;
    }

    private SlaComponentMetricRow componentRow(
        String key,
        String label,
        OperationAggregate agg,
        List<Integer> latencies,
        BigDecimal successTarget,
        int latencyTargetMs
    ) {
        BigDecimal successRate = ratePct(agg.successOps(), agg.totalOps());
        int p50 = percentile(latencies, 50);
        int p95 = percentile(latencies, 95);
        int p99 = percentile(latencies, 99);
        return new SlaComponentMetricRow(
            key,
            label,
            agg.totalOps(),
            agg.successOps(),
            successRate,
            agg.avgMs(),
            p50,
            p95,
            p99,
            agg.maxMs(),
            successTarget,
            latencyTargetMs,
            evaluateSla(successRate, successTarget, p99, latencyTargetMs, agg.totalOps())
        );
    }

    private List<SlaEndpointMetricRow> buildEndpointBreakdown(String tenantId, LocalDate from, LocalDate to) {
        Map<String, OperationAggregate> byOp = queryRepository.getApiAggregatesByOperation(tenantId, from, to);
        List<SlaEndpointMetricRow> rows = new ArrayList<>();
        for (String key : orderedEndpointKeys()) {
            OperationAggregate agg = byOp.getOrDefault(key, OperationAggregate.empty());
            if (agg.totalOps() == 0 && !"EVENT_PROCESS".equals(key) && !"REDEMPTION".equals(key)) {
                continue;
            }
            List<Integer> latencies = latencyForEndpoint(tenantId, from, to, key);
            rows.add(new SlaEndpointMetricRow(
                key,
                ENDPOINT_LABELS.getOrDefault(key, key),
                agg.totalOps(),
                agg.successOps(),
                ratePct(agg.successOps(), agg.totalOps()),
                agg.avgMs(),
                percentile(latencies, 99)
            ));
        }
        rows.sort(Comparator.comparingLong(SlaEndpointMetricRow::requestCount).reversed());
        return rows;
    }

    private List<Integer> latencyForEndpoint(String tenantId, LocalDate from, LocalDate to, String opKey) {
        return switch (opKey) {
            case "EVENT_PROCESS" -> queryRepository.getApiLatencies(tenantId, from, to, "%/events/process%");
            case "REDEMPTION_VALIDATE" -> queryRepository.getApiLatencies(tenantId, from, to, "%/redemptions/validate%");
            case "REDEMPTION" -> queryRepository.getApiLatencies(tenantId, from, to, "%/redemptions%");
            case "BALANCE" -> queryRepository.getApiLatencies(tenantId, from, to, "%/balance%");
            case "EVENT_VALIDATE" -> queryRepository.getApiLatencies(tenantId, from, to, "%/events/validate%");
            default -> queryRepository.getApiLatencies(tenantId, from, to, "%");
        };
    }

    private static List<String> orderedEndpointKeys() {
        return List.of(
            "EVENT_PROCESS",
            "REDEMPTION",
            "REDEMPTION_VALIDATE",
            "BALANCE",
            "EVENT_VALIDATE",
            "OTHER"
        );
    }

    private List<SlaDailyTrendRow> buildDailyTrend(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        List<DailyAggregate> daily = queryRepository.getDailyTrend(tenantId, programmeUid, from, to);
        List<SlaDailyTrendRow> rows = new ArrayList<>();
        for (DailyAggregate day : daily) {
            rows.add(new SlaDailyTrendRow(
                day.period(),
                day.apiRequests(),
                ratePct(day.apiSuccess(), day.apiRequests()),
                day.apiAvgMs(),
                day.issuanceAttempts(),
                ratePct(day.issuanceSuccess(), day.issuanceAttempts()),
                day.issuanceAvgMs(),
                day.eventAttempts(),
                ratePct(day.eventSuccess(), day.eventAttempts()),
                day.eventAvgMs()
            ));
        }
        return rows;
    }

    private static String overallStatus(List<SlaComponentMetricRow> components) {
        boolean anyBreached = components.stream().anyMatch(c -> "BREACHED".equals(c.slaStatus()));
        boolean anyAtRisk = components.stream().anyMatch(c -> "AT_RISK".equals(c.slaStatus()));
        if (anyBreached) {
            return "BREACHED";
        }
        if (anyAtRisk) {
            return "AT_RISK";
        }
        boolean anyData = components.stream().anyMatch(c -> c.totalOperations() > 0);
        return anyData ? "MET" : "NO_DATA";
    }

    private static String evaluateSla(
        BigDecimal successRate,
        BigDecimal successTarget,
        int p99LatencyMs,
        int latencyTargetMs,
        long totalOps
    ) {
        if (totalOps == 0) {
            return "NO_DATA";
        }
        boolean successBreached = successRate.compareTo(successTarget) < 0;
        boolean latencyBreached = p99LatencyMs > latencyTargetMs;
        if (successBreached || latencyBreached) {
            return "BREACHED";
        }
        BigDecimal atRiskThreshold = successTarget.subtract(new BigDecimal("0.5"));
        boolean successAtRisk = successRate.compareTo(atRiskThreshold) < 0;
        boolean latencyAtRisk = p99LatencyMs > (int) (latencyTargetMs * 0.85);
        if (successAtRisk || latencyAtRisk) {
            return "AT_RISK";
        }
        return "MET";
    }

    private static BigDecimal ratePct(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentChange(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(prior).setScale(2, RoundingMode.HALF_UP);
    }

    private static int percentile(List<Integer> values, int pct) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = Math.min(sorted.size() - 1, (int) Math.ceil(pct / 100.0 * sorted.size()) - 1);
        return sorted.get(Math.max(0, index));
    }
}
