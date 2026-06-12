package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.AccrualRedemptionReconciliationResponse;
import com.loyaltyos.analytics.dto.LedgerMovementAggregate;
import com.loyaltyos.analytics.dto.ReconciliationMovementRow;
import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AccrualRedemptionReconciliationService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.010000");
    private static final BigDecimal VARIANCE_TOLERANCE = new BigDecimal("0.01");

    private static final String REPORT_DEFINITION =
        "Liability reconciliation from points_ledger: opening balance (net signed points before the period) "
            + "+ accruals (CREDIT) − redemptions (DEBIT) − expirations (EXPIRE) − reversals (REVERSAL) "
            + "± adjustments (ADJUST, signed) = calculated closing. "
            + "Closing ledger is the net signed balance at period end. "
            + "Cache variance compares customer_balance_cache to ledger (operational check).";

    private static final Map<String, String> ENTRY_LABELS = Map.of(
        "CREDIT", "Accruals (points issued)",
        "DEBIT", "Redemptions",
        "EXPIRE", "Expirations",
        "REVERSAL", "Reversals",
        "ADJUST", "Adjustments"
    );

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public AccrualRedemptionReconciliationService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public AccrualRedemptionReconciliationResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        TenantFinanceContext finance = analyticsQueryRepository.getTenantFinanceContext(tenantId);
        BigDecimal rate = finance.pointsCurrencyRate() != null ? finance.pointsCurrencyRate() : DEFAULT_RATE;
        String currency = finance.currency() != null ? finance.currency() : "INR";

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        BigDecimal opening = analyticsQueryRepository.getNetLedgerBalanceAsOf(tenantId, programmeUid, from);
        BigDecimal closingLedger = analyticsQueryRepository.getNetLedgerBalanceAsOf(
            tenantId,
            programmeUid,
            to.plusDays(1)
        );
        BigDecimal closingCache = analyticsQueryRepository.getOutstandingPointsLiability(tenantId, programmeUid);

        List<LedgerMovementAggregate> aggregates =
            analyticsQueryRepository.getLedgerMovementsInPeriod(tenantId, programmeUid, from, to);
        Map<String, LedgerMovementAggregate> byType = new HashMap<>();
        for (LedgerMovementAggregate agg : aggregates) {
            byType.put(agg.entryType(), agg);
        }

        BigDecimal accruals = magnitude(byType, "CREDIT");
        BigDecimal redemptions = magnitude(byType, "DEBIT");
        BigDecimal expirations = magnitude(byType, "EXPIRE");
        BigDecimal reversals = magnitude(byType, "REVERSAL");
        BigDecimal adjustmentsSigned = signed(byType, "ADJUST");

        BigDecimal netChange = aggregates.stream()
            .map(LedgerMovementAggregate::signedPointsImpact)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal calculatedClosing = opening.add(netChange);
        BigDecimal waterfallVariance = closingLedger.subtract(calculatedClosing);
        BigDecimal cacheVsLedger = closingCache.subtract(closingLedger);

        BigDecimal priorNetChange = netChangeInRange(tenantId, programmeUid, priorFrom, priorTo);

        AccrualRedemptionReconciliationResponse report = new AccrualRedemptionReconciliationResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setCurrency(currency);
        report.setPointsCurrencyRate(rate);
        report.setReportDefinition(REPORT_DEFINITION);

        report.setOpeningPointsLiability(opening);
        report.setOpeningMonetaryLiability(toMonetary(opening, rate));
        report.setClosingPointsLiabilityLedger(closingLedger);
        report.setClosingMonetaryLiabilityLedger(toMonetary(closingLedger, rate));
        report.setClosingPointsLiabilityCache(closingCache);
        report.setClosingMonetaryLiabilityCache(toMonetary(closingCache, rate));
        report.setCalculatedClosingPoints(calculatedClosing);
        report.setWaterfallVariancePoints(waterfallVariance);
        report.setCacheVsLedgerVariancePoints(cacheVsLedger);
        report.setReconciliationStatus(resolveStatus(waterfallVariance, cacheVsLedger));

        report.setAccrualsPoints(accruals);
        report.setRedemptionsPoints(redemptions);
        report.setExpirationsPoints(expirations);
        report.setReversalsPoints(reversals);
        report.setAdjustmentsNetPoints(adjustmentsSigned);
        report.setNetChangePoints(netChange);
        report.setNetChangeMonetary(toMonetary(netChange, rate));
        report.setPriorPeriodNetChangePoints(priorNetChange);
        report.setPeriodOverPeriodNetChangePct(percentChange(netChange, priorNetChange));

        long totalTx = aggregates.stream().mapToLong(LedgerMovementAggregate::transactionCount).sum();
        report.setTotalTransactionsInPeriod(totalTx);
        report.setUniqueCustomersInPeriod(
            analyticsQueryRepository.countUniqueLedgerCustomersInPeriod(tenantId, programmeUid, from, to)
        );

        report.setMovements(buildMovementRows(aggregates, rate));
        report.setDailyTrend(analyticsQueryRepository.getDailyReconciliationTrend(tenantId, programmeUid, from, to));
        report.setRecentBalanceVariances(
            analyticsQueryRepository.getRecentBalanceVariances(tenantId, programmeUid, from, to, 25)
        );
        return report;
    }

    private BigDecimal netChangeInRange(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        return analyticsQueryRepository.getLedgerMovementsInPeriod(tenantId, programmeUid, from, to).stream()
            .map(LedgerMovementAggregate::signedPointsImpact)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<ReconciliationMovementRow> buildMovementRows(
        List<LedgerMovementAggregate> aggregates,
        BigDecimal rate
    ) {
        List<String> order = List.of("CREDIT", "DEBIT", "EXPIRE", "REVERSAL", "ADJUST");
        Map<String, LedgerMovementAggregate> byType = new HashMap<>();
        for (LedgerMovementAggregate agg : aggregates) {
            byType.put(agg.entryType(), agg);
        }
        List<ReconciliationMovementRow> rows = new ArrayList<>();
        for (String type : order) {
            LedgerMovementAggregate agg = byType.get(type);
            if (agg == null) {
                continue;
            }
            ReconciliationMovementRow row = new ReconciliationMovementRow();
            row.setEntryType(type);
            row.setLabel(ENTRY_LABELS.getOrDefault(type, type));
            row.setPointsMagnitude(nullToZero(agg.pointsMagnitude()));
            row.setSignedPointsImpact(nullToZero(agg.signedPointsImpact()));
            row.setTransactionCount(agg.transactionCount());
            row.setUniqueCustomers(agg.uniqueCustomers());
            row.setMonetaryValue(toMonetary(nullToZero(agg.pointsMagnitude()), rate));
            rows.add(row);
        }
        return rows;
    }

    private static BigDecimal magnitude(Map<String, LedgerMovementAggregate> byType, String type) {
        LedgerMovementAggregate agg = byType.get(type);
        return agg == null ? BigDecimal.ZERO : nullToZero(agg.pointsMagnitude());
    }

    private static BigDecimal signed(Map<String, LedgerMovementAggregate> byType, String type) {
        LedgerMovementAggregate agg = byType.get(type);
        return agg == null ? BigDecimal.ZERO : nullToZero(agg.signedPointsImpact());
    }

    private static String resolveStatus(BigDecimal waterfallVariance, BigDecimal cacheVsLedger) {
        if (withinTolerance(waterfallVariance) && withinTolerance(cacheVsLedger)) {
            return "BALANCED";
        }
        return "VARIANCE_DETECTED";
    }

    private static boolean withinTolerance(BigDecimal value) {
        return value == null || value.abs().compareTo(VARIANCE_TOLERANCE) <= 0;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal toMonetary(BigDecimal points, BigDecimal rate) {
        if (points == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return points.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentChange(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.signum() == 0) {
            return current != null && current.signum() != 0 ? null : BigDecimal.ZERO;
        }
        return current.subtract(prior)
            .multiply(new BigDecimal("100"))
            .divide(prior, 1, RoundingMode.HALF_UP);
    }
}
