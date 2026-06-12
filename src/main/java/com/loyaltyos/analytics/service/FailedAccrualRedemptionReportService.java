package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.FailedAccrualRedemptionReportResponse;
import com.loyaltyos.analytics.dto.FailedTransactionRow;
import com.loyaltyos.analytics.dto.FailureCategoryRow;
import com.loyaltyos.analytics.repository.FailedAccrualRedemptionQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FailedAccrualRedemptionReportService {

    private static final int RECENT_LIMIT = 200;

    private static final String REPORT_DEFINITION =
        "Operational report (BRD §6.3): failed point accruals from reward_issuance_audit and "
            + "integration_event_processing_log; failed redemptions from integration API audit log (POST /redemptions). "
            + "Accrual issuance rows are filtered by programme; event-processing and redemption API rows are tenant-wide.";

    private final FailedAccrualRedemptionQueryRepository queryRepository;

    public FailedAccrualRedemptionReportService(FailedAccrualRedemptionQueryRepository queryRepository) {
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository");
    }

    public FailedAccrualRedemptionReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        long issuanceFailed = queryRepository.countIssuanceAuditByStatus(
            tenantId, programmeUid, "FAILED", from, to
        );
        long eventFailed = queryRepository.countFailedEventProcessing(tenantId, from, to);
        long redemptionFailed = queryRepository.countRedemptionApiAttempts(tenantId, from, to, true);

        long accrualFailed = issuanceFailed + eventFailed;
        long totalFailed = accrualFailed + redemptionFailed;

        long priorIssuanceFailed = queryRepository.countIssuanceAuditByStatus(
            tenantId, programmeUid, "FAILED", priorFrom, priorTo
        );
        long priorEventFailed = queryRepository.countFailedEventProcessing(tenantId, priorFrom, priorTo);
        long priorRedemptionFailed = queryRepository.countRedemptionApiAttempts(tenantId, priorFrom, priorTo, true);
        long priorTotal = priorIssuanceFailed + priorEventFailed + priorRedemptionFailed;

        long accrualAttempts = queryRepository.countIssuanceAuditByStatus(tenantId, programmeUid, "SUCCESS", from, to)
            + issuanceFailed;
        long redemptionAttempts = queryRepository.countRedemptionApiAttempts(tenantId, from, to, false);

        List<FailedTransactionRow> recent = mergeRecentFailures(tenantId, programmeUid, from, to);
        for (FailedTransactionRow row : recent) {
            row.setErrorCategory(categorize(row));
        }

        FailedAccrualRedemptionReportResponse report = new FailedAccrualRedemptionReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setReportDefinition(REPORT_DEFINITION);

        report.setFailedAccrualsInPeriod(accrualFailed);
        report.setFailedRedemptionsInPeriod(redemptionFailed);
        report.setTotalFailuresInPeriod(totalFailed);
        report.setFailedAccrualsPriorPeriod(priorIssuanceFailed + priorEventFailed);
        report.setFailedRedemptionsPriorPeriod(priorRedemptionFailed);
        report.setPeriodOverPeriodChangePct(percentChange(totalFailed, priorTotal));

        report.setAccrualAttemptsInPeriod(accrualAttempts);
        report.setRedemptionApiAttemptsInPeriod(redemptionAttempts);
        report.setAccrualFailureRatePct(ratePct(accrualFailed, accrualAttempts));
        report.setRedemptionFailureRatePct(ratePct(redemptionFailed, redemptionAttempts));

        report.setAvgFailedAccrualDurationMs(toRoundedInt(queryRepository.avgFailedIssuanceDurationMs(
            tenantId, programmeUid, from, to
        )));
        report.setAvgFailedRedemptionDurationMs(toRoundedInt(queryRepository.avgFailedRedemptionDurationMs(
            tenantId, from, to
        )));

        report.setFailuresByCategory(buildCategoryBreakdown(recent));
        report.setDailyTrend(queryRepository.getDailyFailureTrend(tenantId, programmeUid, from, to));
        report.setRecentFailures(recent);
        return report;
    }

    private List<FailedTransactionRow> mergeRecentFailures(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        List<FailedTransactionRow> issuance = queryRepository.getFailedIssuanceAuditRows(
            tenantId, programmeUid, from, to, RECENT_LIMIT
        );
        List<FailedTransactionRow> events = queryRepository.getFailedEventProcessingRows(
            tenantId, from, to, RECENT_LIMIT
        );
        List<FailedTransactionRow> redemptions = queryRepository.getFailedRedemptionApiRows(
            tenantId, from, to, RECENT_LIMIT
        );

        Set<String> seenAccrualRefs = new HashSet<>();
        for (FailedTransactionRow row : issuance) {
            if (row.getReferenceId() != null) {
                seenAccrualRefs.add(row.getReferenceId());
            }
        }

        List<FailedTransactionRow> merged = new ArrayList<>(issuance);
        for (FailedTransactionRow row : events) {
            if (row.getReferenceId() != null && seenAccrualRefs.contains(row.getReferenceId())) {
                continue;
            }
            merged.add(row);
        }
        merged.addAll(redemptions);

        merged.sort(Comparator.comparing(
            FailedTransactionRow::getOccurredAt,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        if (merged.size() > RECENT_LIMIT) {
            return new ArrayList<>(merged.subList(0, RECENT_LIMIT));
        }
        return merged;
    }

    private static List<FailureCategoryRow> buildCategoryBreakdown(List<FailedTransactionRow> rows) {
        Map<String, Long> counts = new HashMap<>();
        for (FailedTransactionRow row : rows) {
            String key = row.getTransactionType() + "|" + row.getErrorCategory();
            counts.merge(key, 1L, Long::sum);
        }
        List<FailureCategoryRow> result = new ArrayList<>();
        counts.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                String[] parts = entry.getKey().split("\\|", 2);
                result.add(new FailureCategoryRow(parts[1], parts[0], entry.getValue()));
            });
        return result;
    }

    static String categorize(FailedTransactionRow row) {
        String code = row.getErrorCode() != null ? row.getErrorCode().toUpperCase(Locale.ROOT) : "";
        String message = row.getErrorMessage() != null ? row.getErrorMessage().toUpperCase(Locale.ROOT) : "";

        if (code.contains("INSUFFICIENT_BALANCE") || message.contains("INSUFFICIENT BALANCE")) {
            return "Insufficient balance";
        }
        if (code.contains("REDEMPTION_LIMIT") || message.contains("LIMIT")) {
            return "Limit exceeded";
        }
        if (code.contains("VALIDATION") || message.contains("VALIDATION")) {
            return "Validation error";
        }
        if (code.contains("RULE_ERROR") || message.contains("RULE")) {
            return "Rule evaluation";
        }
        if (code.contains("PROGRAMME_INACTIVE") || message.contains("PROGRAMME")) {
            return "Programme inactive";
        }
        if (code.contains("SYSTEM") || message.contains("SYSTEM")) {
            return "System error";
        }
        if ("ACCRUAL".equals(row.getTransactionType())) {
            return "Issuance failure";
        }
        return "Other";
    }

    private static BigDecimal percentChange(long current, long prior) {
        if (prior == 0L) {
            return current == 0L ? BigDecimal.ZERO : null;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratePct(long failed, long attempts) {
        if (attempts <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(failed)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(attempts), 1, RoundingMode.HALF_UP);
    }

    private static Integer toRoundedInt(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return (int) Math.round(value);
    }
}
