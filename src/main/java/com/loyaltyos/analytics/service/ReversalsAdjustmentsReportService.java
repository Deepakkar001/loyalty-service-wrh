package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.LedgerMovementAggregate;
import com.loyaltyos.analytics.dto.ReversalsAdjustmentsReportResponse;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.analytics.repository.ReversalsAdjustmentsQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReversalsAdjustmentsReportService {

    private static final int ROW_LIMIT = 200;
    private static final int TOP_CUSTOMERS_LIMIT = 10;

    private static final String REPORT_DEFINITION =
        "Operational report (BRD §6.3): line-level REVERSAL and ADJUST rows from points_ledger. "
            + "Reversals undo prior CREDIT issuances and link to the original ledger row via reversal_of_ledger_id. "
            + "Adjustments use signed points (positive increases liability, negative decreases).";

    private final ReversalsAdjustmentsQueryRepository reversalsAdjustmentsQueryRepository;
    private final AnalyticsQueryRepository analyticsQueryRepository;

    public ReversalsAdjustmentsReportService(
        ReversalsAdjustmentsQueryRepository reversalsAdjustmentsQueryRepository,
        AnalyticsQueryRepository analyticsQueryRepository
    ) {
        this.reversalsAdjustmentsQueryRepository = Objects.requireNonNull(
            reversalsAdjustmentsQueryRepository,
            "reversalsAdjustmentsQueryRepository"
        );
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public ReversalsAdjustmentsReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        Map<String, LedgerMovementAggregate> movements = movementsByType(tenantId, programmeUid, from, to);
        BigDecimal reversalPoints = magnitude(movements, "REVERSAL");
        BigDecimal adjustmentNet = signed(movements, "ADJUST");
        long reversalCount = count(movements, "REVERSAL");
        long adjustmentCount = count(movements, "ADJUST");

        long priorReversalCount = reversalsAdjustmentsQueryRepository.countEntries(
            tenantId, programmeUid, "REVERSAL", priorFrom, priorTo
        );
        long priorAdjustmentCount = reversalsAdjustmentsQueryRepository.countEntries(
            tenantId, programmeUid, "ADJUST", priorFrom, priorTo
        );

        ReversalsAdjustmentsReportResponse report = new ReversalsAdjustmentsReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setReportDefinition(REPORT_DEFINITION);

        report.setReversalCountInPeriod(reversalCount);
        report.setReversalPointsInPeriod(reversalPoints);
        report.setAdjustmentCountInPeriod(adjustmentCount);
        report.setAdjustmentNetPointsInPeriod(adjustmentNet);
        report.setUniqueCustomersAffected(
            reversalsAdjustmentsQueryRepository.countUniqueCustomers(tenantId, programmeUid, from, to)
        );
        report.setReversalCountPriorPeriod(priorReversalCount);
        report.setAdjustmentCountPriorPeriod(priorAdjustmentCount);
        report.setPeriodOverPeriodReversalChangePct(percentChange(reversalCount, priorReversalCount));
        report.setPeriodOverPeriodAdjustmentChangePct(percentChange(adjustmentCount, priorAdjustmentCount));

        report.setDailyTrend(reversalsAdjustmentsQueryRepository.getDailyTrend(tenantId, programmeUid, from, to));
        report.setTopCustomers(reversalsAdjustmentsQueryRepository.getTopCustomers(
            tenantId, programmeUid, from, to, TOP_CUSTOMERS_LIMIT
        ));
        report.setReversals(reversalsAdjustmentsQueryRepository.getReversalRows(
            tenantId, programmeUid, from, to, ROW_LIMIT
        ));
        report.setAdjustments(reversalsAdjustmentsQueryRepository.getAdjustmentRows(
            tenantId, programmeUid, from, to, ROW_LIMIT
        ));
        return report;
    }

    private Map<String, LedgerMovementAggregate> movementsByType(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        List<LedgerMovementAggregate> aggregates =
            analyticsQueryRepository.getLedgerMovementsInPeriod(tenantId, programmeUid, from, to);
        Map<String, LedgerMovementAggregate> byType = new HashMap<>();
        for (LedgerMovementAggregate agg : aggregates) {
            if ("REVERSAL".equals(agg.entryType()) || "ADJUST".equals(agg.entryType())) {
                byType.put(agg.entryType(), agg);
            }
        }
        return byType;
    }

    private static BigDecimal magnitude(Map<String, LedgerMovementAggregate> byType, String entryType) {
        LedgerMovementAggregate agg = byType.get(entryType);
        return agg == null || agg.pointsMagnitude() == null ? BigDecimal.ZERO : agg.pointsMagnitude();
    }

    private static BigDecimal signed(Map<String, LedgerMovementAggregate> byType, String entryType) {
        LedgerMovementAggregate agg = byType.get(entryType);
        return agg == null || agg.signedPointsImpact() == null ? BigDecimal.ZERO : agg.signedPointsImpact();
    }

    private static long count(Map<String, LedgerMovementAggregate> byType, String entryType) {
        LedgerMovementAggregate agg = byType.get(entryType);
        return agg == null ? 0L : agg.transactionCount();
    }

    private static BigDecimal percentChange(long current, long prior) {
        if (prior == 0L) {
            return current == 0L ? BigDecimal.ZERO : null;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP);
    }
}
