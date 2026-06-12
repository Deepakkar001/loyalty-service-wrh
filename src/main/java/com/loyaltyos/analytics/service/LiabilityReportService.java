package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.LedgerMovementAggregate;
import com.loyaltyos.analytics.dto.LiabilityMonthlyMovementRow;
import com.loyaltyos.analytics.dto.LiabilityProgrammeRollupRow;
import com.loyaltyos.analytics.dto.LiabilityReportResponse;
import com.loyaltyos.analytics.dto.LiabilityTierBreakdownRow;
import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository.ProgrammePeriodMovement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class LiabilityReportService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.010000");

    private static final String REPORT_DEFINITION =
        "Loyalty liability report (BRD §4.17): outstanding points valued at the tenant conversion rate, "
            + "monthly movement (opening → issued → redeemed → expired → closing) from points_ledger, "
            + "programme and tenant roll-ups, and tier breakdown from customer balance cache.";

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public LiabilityReportService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public LiabilityReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        TenantFinanceContext finance = analyticsQueryRepository.getTenantFinanceContext(tenantId);
        BigDecimal rate = finance.pointsCurrencyRate() != null ? finance.pointsCurrencyRate() : DEFAULT_RATE;
        String currency = finance.currency() != null ? finance.currency() : "INR";

        BigDecimal outstandingCache = analyticsQueryRepository.getOutstandingPointsLiability(tenantId, programmeUid);
        BigDecimal ledgerClosing = analyticsQueryRepository.getNetLedgerBalanceAsOf(
            tenantId,
            programmeUid,
            to.plusDays(1)
        );
        BigDecimal opening = analyticsQueryRepository.getNetLedgerBalanceAsOf(tenantId, programmeUid, from);

        List<LedgerMovementAggregate> periodMovements =
            analyticsQueryRepository.getLedgerMovementsInPeriod(tenantId, programmeUid, from, to);
        Map<String, LedgerMovementAggregate> byType = new HashMap<>();
        for (LedgerMovementAggregate agg : periodMovements) {
            byType.put(agg.entryType(), agg);
        }

        BigDecimal issued = magnitude(byType, "CREDIT");
        BigDecimal redeemed = magnitude(byType, "DEBIT");
        BigDecimal expired = magnitude(byType, "EXPIRE");
        BigDecimal reversed = magnitude(byType, "REVERSAL");
        BigDecimal adjustmentsNet = signed(byType, "ADJUST");
        BigDecimal netChange = periodMovements.stream()
            .map(LedgerMovementAggregate::signedPointsImpact)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closing = opening.add(netChange);
        long totalTx = periodMovements.stream().mapToLong(LedgerMovementAggregate::transactionCount).sum();

        LiabilityReportResponse report = new LiabilityReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setCurrency(currency);
        report.setPointsCurrencyRate(rate);
        report.setReportDefinition(REPORT_DEFINITION);

        report.setOutstandingPointsLiability(nullToZero(outstandingCache));
        report.setOutstandingMonetaryLiability(toMonetary(outstandingCache, rate));
        report.setLedgerClosingPoints(nullToZero(ledgerClosing));
        report.setLedgerClosingMonetary(toMonetary(ledgerClosing, rate));
        report.setLedgerVsCacheVariancePoints(nullToZero(outstandingCache).subtract(nullToZero(ledgerClosing)));

        report.setPeriodPointsIssued(issued);
        report.setPeriodPointsRedeemed(redeemed);
        report.setPeriodPointsExpired(expired);
        report.setPeriodPointsReversed(reversed);
        report.setPeriodAdjustmentsNet(adjustmentsNet);
        report.setPeriodNetChangePoints(netChange);
        report.setPeriodNetChangeMonetary(toMonetary(netChange, rate));

        report.setOpeningPointsLiability(nullToZero(opening));
        report.setOpeningMonetaryLiability(toMonetary(opening, rate));
        report.setClosingPointsLiability(nullToZero(closing));
        report.setClosingMonetaryLiability(toMonetary(closing, rate));

        report.setMembersWithBalance(analyticsQueryRepository.countMembersWithBalance(tenantId, programmeUid));
        report.setTotalLedgerTransactionsInPeriod(totalTx);

        report.setMonthlyMovement(buildMonthlyMovement(tenantId, programmeUid, from, to, rate));
        report.setProgrammeRollups(buildProgrammeRollups(tenantId, from, to, rate));
        report.setTenantRollup(buildTenantRollup(report.getProgrammeRollups(), rate));
        report.setLiabilityByTier(buildTierBreakdown(tenantId, programmeUid, rate));

        return report;
    }

    private List<LiabilityMonthlyMovementRow> buildMonthlyMovement(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        BigDecimal rate
    ) {
        List<LiabilityMonthlyMovementRow> rows = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);

        while (!cursor.isAfter(end)) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd = cursor.atEndOfMonth();
            LocalDate periodStart = from.isAfter(monthStart) ? from : monthStart;
            LocalDate periodEnd = to.isBefore(monthEnd) ? to : monthEnd;
            boolean partial = !periodStart.equals(monthStart) || !periodEnd.equals(monthEnd);

            BigDecimal opening = analyticsQueryRepository.getNetLedgerBalanceAsOf(tenantId, programmeUid, periodStart);
            List<LedgerMovementAggregate> movements = analyticsQueryRepository.getLedgerMovementsInPeriod(
                tenantId,
                programmeUid,
                periodStart,
                periodEnd
            );
            Map<String, LedgerMovementAggregate> byType = new HashMap<>();
            for (LedgerMovementAggregate agg : movements) {
                byType.put(agg.entryType(), agg);
            }

            BigDecimal issued = magnitude(byType, "CREDIT");
            BigDecimal redeemed = magnitude(byType, "DEBIT");
            BigDecimal expired = magnitude(byType, "EXPIRE");
            BigDecimal reversed = magnitude(byType, "REVERSAL");
            BigDecimal adjustmentsNet = signed(byType, "ADJUST");
            BigDecimal netChange = movements.stream()
                .map(LedgerMovementAggregate::signedPointsImpact)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal closing = nullToZero(opening).add(netChange);

            rows.add(new LiabilityMonthlyMovementRow(
                cursor.toString(),
                partial,
                nullToZero(opening),
                toMonetary(opening, rate),
                issued,
                redeemed,
                expired,
                reversed,
                adjustmentsNet,
                netChange,
                toMonetary(netChange, rate),
                closing,
                toMonetary(closing, rate)
            ));
            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    private List<LiabilityProgrammeRollupRow> buildProgrammeRollups(
        String tenantId,
        LocalDate from,
        LocalDate to,
        BigDecimal rate
    ) {
        Map<String, BigDecimal> outstandingByProgramme =
            analyticsQueryRepository.getOutstandingPointsByProgramme(tenantId);
        Map<String, Long> membersByProgramme = analyticsQueryRepository.getMemberCountByProgramme(tenantId);
        Map<String, ProgrammePeriodMovement> movements =
            analyticsQueryRepository.getProgrammePeriodMovements(tenantId, from, to);

        Set<String> programmeUids = new TreeSet<>();
        programmeUids.addAll(outstandingByProgramme.keySet());
        programmeUids.addAll(membersByProgramme.keySet());
        programmeUids.addAll(movements.keySet());

        List<LiabilityProgrammeRollupRow> rows = new ArrayList<>();
        for (String programmeUid : programmeUids) {
            BigDecimal outstanding = outstandingByProgramme.getOrDefault(programmeUid, BigDecimal.ZERO);
            ProgrammePeriodMovement movement = movements.getOrDefault(programmeUid, new ProgrammePeriodMovement());
            rows.add(new LiabilityProgrammeRollupRow(
                programmeUid,
                membersByProgramme.getOrDefault(programmeUid, 0L),
                nullToZero(outstanding),
                toMonetary(outstanding, rate),
                movement.issued(),
                movement.redeemed(),
                movement.expired(),
                movement.netChange(),
                toMonetary(movement.netChange(), rate)
            ));
        }
        return rows;
    }

    private static LiabilityProgrammeRollupRow buildTenantRollup(
        List<LiabilityProgrammeRollupRow> rollups,
        BigDecimal rate
    ) {
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal issued = BigDecimal.ZERO;
        BigDecimal redeemed = BigDecimal.ZERO;
        BigDecimal expired = BigDecimal.ZERO;
        BigDecimal netChange = BigDecimal.ZERO;
        long members = 0L;
        for (LiabilityProgrammeRollupRow row : rollups) {
            outstanding = outstanding.add(nullToZero(row.outstandingPoints()));
            issued = issued.add(nullToZero(row.periodPointsIssued()));
            redeemed = redeemed.add(nullToZero(row.periodPointsRedeemed()));
            expired = expired.add(nullToZero(row.periodPointsExpired()));
            netChange = netChange.add(nullToZero(row.periodNetChangePoints()));
            members += row.memberCount();
        }
        return new LiabilityProgrammeRollupRow(
            "TENANT_TOTAL",
            members,
            outstanding,
            toMonetary(outstanding, rate),
            issued,
            redeemed,
            expired,
            netChange,
            toMonetary(netChange, rate)
        );
    }

    private List<LiabilityTierBreakdownRow> buildTierBreakdown(
        String tenantId,
        String programmeUid,
        BigDecimal rate
    ) {
        List<LiabilityTierBreakdownRow> tiers = analyticsQueryRepository.getLiabilityByTier(tenantId, programmeUid);
        List<LiabilityTierBreakdownRow> result = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (LiabilityTierBreakdownRow tier : tiers) {
            BigDecimal points = nullToZero(tier.pointsLiability());
            assigned = assigned.add(points);
            result.add(new LiabilityTierBreakdownRow(
                tier.tierName(),
                tier.rankOrder(),
                tier.memberCount(),
                points,
                toMonetary(points, rate)
            ));
        }

        BigDecimal totalOutstanding = analyticsQueryRepository.getOutstandingPointsLiability(tenantId, programmeUid);
        BigDecimal unassigned = nullToZero(totalOutstanding).subtract(assigned);
        if (unassigned.compareTo(BigDecimal.ZERO) > 0) {
            result.add(new LiabilityTierBreakdownRow(
                "Unassigned / below tier",
                9999,
                0L,
                unassigned,
                toMonetary(unassigned, rate)
            ));
        }
        return result;
    }

    private static BigDecimal magnitude(Map<String, LedgerMovementAggregate> byType, String entryType) {
        LedgerMovementAggregate agg = byType.get(entryType);
        return agg == null || agg.pointsMagnitude() == null ? BigDecimal.ZERO : agg.pointsMagnitude();
    }

    private static BigDecimal signed(Map<String, LedgerMovementAggregate> byType, String entryType) {
        LedgerMovementAggregate agg = byType.get(entryType);
        return agg == null || agg.signedPointsImpact() == null ? BigDecimal.ZERO : agg.signedPointsImpact();
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
}
