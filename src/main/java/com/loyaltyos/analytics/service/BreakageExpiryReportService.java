package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.BreakageExpiryReportResponse;
import com.loyaltyos.analytics.dto.ExpirePeriodSummary;
import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class BreakageExpiryReportService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.010000");

    private final AnalyticsQueryRepository analyticsQueryRepository;

    public BreakageExpiryReportService(AnalyticsQueryRepository analyticsQueryRepository) {
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    public BreakageExpiryReportResponse buildReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        TenantFinanceContext finance = analyticsQueryRepository.getTenantFinanceContext(tenantId);
        BigDecimal rate = finance.pointsCurrencyRate() != null ? finance.pointsCurrencyRate() : DEFAULT_RATE;
        String currency = finance.currency() != null ? finance.currency() : "INR";

        ExpirePeriodSummary period = analyticsQueryRepository.getExpirePeriodSummary(tenantId, programmeUid, from, to);
        LocalDate ytdStart = LocalDate.of(to.getYear(), 1, 1);
        ExpirePeriodSummary ytd = analyticsQueryRepository.getExpirePeriodSummary(tenantId, programmeUid, ytdStart, to);
        BigDecimal outstanding = analyticsQueryRepository.getOutstandingPointsLiability(tenantId, programmeUid);

        BreakageExpiryReportResponse report = new BreakageExpiryReportResponse();
        report.setProgrammeUid(programmeUid);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setCurrency(currency);
        report.setPointsCurrencyRate(rate);

        report.setPointsExpiredInPeriod(nullToZero(period.totalPoints()));
        report.setCustomersAffectedInPeriod(period.customersAffected());
        report.setExpireTransactionCount(period.transactionCount());
        report.setMonetaryBreakageInPeriod(toMonetary(period.totalPoints(), rate));

        report.setPointsExpiredYtd(nullToZero(ytd.totalPoints()));
        report.setMonetaryBreakageYtd(toMonetary(ytd.totalPoints(), rate));

        report.setOutstandingPointsLiability(nullToZero(outstanding));
        report.setOutstandingMonetaryLiability(toMonetary(outstanding, rate));

        report.setPointsExpiringNext30Days(
            nullToZero(analyticsQueryRepository.getUpcomingExpiryPoints(tenantId, programmeUid, 30))
        );
        report.setPointsExpiringNext60Days(
            nullToZero(analyticsQueryRepository.getUpcomingExpiryPoints(tenantId, programmeUid, 60))
        );
        report.setPointsExpiringNext90Days(
            nullToZero(analyticsQueryRepository.getUpcomingExpiryPoints(tenantId, programmeUid, 90))
        );

        report.setMonthlyBreakage(analyticsQueryRepository.getMonthlyBreakage(tenantId, programmeUid, from, to));
        report.setBreakageByTier(analyticsQueryRepository.getBreakageByTier(tenantId, programmeUid, from, to));
        report.setUpcomingExpiryByMonth(analyticsQueryRepository.getUpcomingExpiryByMonth(tenantId, programmeUid, 12));
        report.setRecentExpiryJobRuns(
            analyticsQueryRepository.getRecentExpiryJobRuns(tenantId, programmeUid, 10)
        );

        return report;
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
