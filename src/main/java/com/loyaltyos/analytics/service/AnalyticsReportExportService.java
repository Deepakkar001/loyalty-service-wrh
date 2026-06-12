package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.AccrualRedemptionReconciliationResponse;
import com.loyaltyos.analytics.dto.BalanceReconciliationVarianceRow;
import com.loyaltyos.analytics.dto.BreakageExpiryReportResponse;
import com.loyaltyos.analytics.dto.BreakageMonthlyRow;
import com.loyaltyos.analytics.dto.BreakageTierRow;
import com.loyaltyos.analytics.dto.CohortRetentionRow;
import com.loyaltyos.analytics.dto.EnrollmentReportResponse;
import com.loyaltyos.analytics.dto.EnrollmentRuleRow;
import com.loyaltyos.analytics.dto.EnrollmentSourceRow;
import com.loyaltyos.analytics.dto.EnrollmentTrendRow;
import com.loyaltyos.analytics.dto.ExpiryJobRunRow;
import com.loyaltyos.analytics.dto.PointsActivityRow;
import com.loyaltyos.analytics.dto.ReconciliationDailyRow;
import com.loyaltyos.analytics.dto.ReconciliationMovementRow;
import com.loyaltyos.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.analytics.dto.RulePerformanceRow;
import com.loyaltyos.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.analytics.dto.TierDistributionRow;
import com.loyaltyos.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.analytics.dto.TierVelocityBucketRow;
import com.loyaltyos.analytics.dto.UpcomingExpiryMonthRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsReportExportService {

    private final AccrualRedemptionReconciliationService accrualRedemptionReconciliationService;
    private final BreakageExpiryReportService breakageExpiryReportService;
    private final EnrollmentReportService enrollmentReportService;
    private final AnalyticsService analyticsService;

    public AnalyticsReportExportService(
        AccrualRedemptionReconciliationService accrualRedemptionReconciliationService,
        BreakageExpiryReportService breakageExpiryReportService,
        EnrollmentReportService enrollmentReportService,
        AnalyticsService analyticsService
    ) {
        this.accrualRedemptionReconciliationService = Objects.requireNonNull(
            accrualRedemptionReconciliationService,
            "accrualRedemptionReconciliationService"
        );
        this.breakageExpiryReportService = Objects.requireNonNull(
            breakageExpiryReportService,
            "breakageExpiryReportService"
        );
        this.enrollmentReportService = Objects.requireNonNull(enrollmentReportService, "enrollmentReportService");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    public void streamAccrualRedemptionReconciliation(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        AccrualRedemptionReconciliationResponse report =
            accrualRedemptionReconciliationService.buildReport(tenantId, programmeUid, from, to);
        lineConsumer.accept(join(
            "SUMMARY",
            report.getReconciliationStatus(),
            decimal(report.getOpeningPointsLiability()),
            decimal(report.getAccrualsPoints()),
            decimal(report.getRedemptionsPoints()),
            decimal(report.getExpirationsPoints()),
            decimal(report.getReversalsPoints()),
            decimal(report.getAdjustmentsNetPoints()),
            decimal(report.getCalculatedClosingPoints()),
            decimal(report.getClosingPointsLiabilityLedger()),
            decimal(report.getWaterfallVariancePoints()),
            decimal(report.getCacheVsLedgerVariancePoints()),
            decimal(report.getNetChangePoints()),
            decimal(report.getNetChangeMonetary()),
            String.valueOf(report.getTotalTransactionsInPeriod()),
            String.valueOf(report.getUniqueCustomersInPeriod())
        ));
        for (ReconciliationMovementRow row : report.getMovements()) {
            lineConsumer.accept(join(
                "MOVEMENT",
                row.getEntryType(),
                row.getLabel(),
                decimal(row.getPointsMagnitude()),
                decimal(row.getSignedPointsImpact()),
                String.valueOf(row.getTransactionCount()),
                String.valueOf(row.getUniqueCustomers()),
                decimal(row.getMonetaryValue())
            ));
        }
        for (ReconciliationDailyRow row : report.getDailyTrend()) {
            lineConsumer.accept(join(
                "DAILY",
                row.period(),
                decimal(row.accruals()),
                decimal(row.redemptions()),
                decimal(row.expirations()),
                decimal(row.reversals()),
                decimal(row.adjustments()),
                decimal(row.netChange())
            ));
        }
        for (BalanceReconciliationVarianceRow row : report.getRecentBalanceVariances()) {
            lineConsumer.accept(join(
                "VARIANCE",
                row.getCustomerId(),
                decimal(row.getExpectedBalance()),
                decimal(row.getCachedBalance()),
                decimal(row.getVariance()),
                row.getReconciliationAction(),
                row.getExecutedAt() == null ? "" : row.getExecutedAt().toString()
            ));
        }
    }

    public void streamBreakageExpiry(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        BreakageExpiryReportResponse report =
            breakageExpiryReportService.buildReport(tenantId, programmeUid, from, to);
        lineConsumer.accept(join(
            "SUMMARY",
            decimal(report.getPointsExpiredInPeriod()),
            String.valueOf(report.getCustomersAffectedInPeriod()),
            String.valueOf(report.getExpireTransactionCount()),
            decimal(report.getMonetaryBreakageInPeriod()),
            decimal(report.getPointsExpiredYtd()),
            decimal(report.getOutstandingPointsLiability()),
            decimal(report.getPointsExpiringNext30Days()),
            decimal(report.getPointsExpiringNext60Days()),
            decimal(report.getPointsExpiringNext90Days())
        ));
        for (BreakageMonthlyRow row : report.getMonthlyBreakage()) {
            lineConsumer.accept(join(
                "MONTHLY",
                row.getMonth(),
                decimal(row.getExpiredPoints()),
                String.valueOf(row.getCustomersAffected()),
                String.valueOf(row.getTransactionCount())
            ));
        }
        for (BreakageTierRow row : report.getBreakageByTier()) {
            lineConsumer.accept(join(
                "TIER",
                row.getTierName(),
                String.valueOf(row.getRankOrder()),
                decimal(row.getExpiredPoints()),
                String.valueOf(row.getCustomersAffected())
            ));
        }
        for (UpcomingExpiryMonthRow row : report.getUpcomingExpiryByMonth()) {
            lineConsumer.accept(join(
                "UPCOMING",
                row.getExpiryMonth(),
                decimal(row.getPointsExpiring()),
                String.valueOf(row.getCustomersAffected())
            ));
        }
        for (ExpiryJobRunRow row : report.getRecentExpiryJobRuns()) {
            lineConsumer.accept(join(
                "JOB_RUN",
                row.getBatchDate(),
                row.getStatus(),
                row.getTotalExpired() == null ? "" : row.getTotalExpired().toString(),
                row.getCustomersAffected() == null ? "" : row.getCustomersAffected().toString(),
                row.getExecutedAt() == null ? "" : row.getExecutedAt()
            ));
        }
    }

    public void streamEnrollment(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        EnrollmentReportResponse report = enrollmentReportService.buildReport(tenantId, programmeUid, from, to);
        lineConsumer.accept(join(
            "SUMMARY",
            String.valueOf(report.getNewEnrollmentsInPeriod()),
            String.valueOf(report.getNewEnrollmentsPriorPeriod()),
            String.valueOf(report.getTotalEnrolledMembers()),
            String.valueOf(report.getReturningActiveInPeriod()),
            String.valueOf(report.getNewEnrollmentsYtd()),
            report.getPeriodOverPeriodChangePct() == null ? "" : report.getPeriodOverPeriodChangePct().toString()
        ));
        for (EnrollmentTrendRow row : report.getDailyNewEnrollments()) {
            lineConsumer.accept(join("DAILY", row.getPeriod(), String.valueOf(row.getNewEnrollments())));
        }
        for (EnrollmentTrendRow row : report.getMonthlyNewEnrollments()) {
            lineConsumer.accept(join("MONTHLY", row.getPeriod(), String.valueOf(row.getNewEnrollments())));
        }
        for (EnrollmentSourceRow row : report.getEnrollmentsBySource()) {
            lineConsumer.accept(join(
                "SOURCE",
                row.getSourceType(),
                String.valueOf(row.getNewEnrollments())
            ));
        }
        for (EnrollmentRuleRow row : report.getTopEnrollmentRules()) {
            lineConsumer.accept(join(
                "RULE",
                row.getRuleUid(),
                row.getRuleName(),
                String.valueOf(row.getNewEnrollments())
            ));
        }
    }

    public void streamCustomReports(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        for (PointsActivityRow row : analyticsService.getPointsActivity(tenantId, programmeUid, from, to)) {
            lineConsumer.accept(join(
                "POINTS_ACTIVITY",
                row.reportDate(),
                row.entryType(),
                String.valueOf(row.transactionCount()),
                decimal(row.totalPoints()),
                String.valueOf(row.uniqueCustomers())
            ));
        }
        for (RulePerformanceRow row : analyticsService.getRulePerformance(tenantId, programmeUid, from, to)) {
            lineConsumer.accept(join(
                "RULE_PERFORMANCE",
                row.ruleUid(),
                row.ruleName(),
                row.status(),
                String.valueOf(row.evaluationCount()),
                String.valueOf(row.successCount()),
                decimal(row.totalPointsAwarded())
            ));
        }
        for (TierDistributionRow row : analyticsService.getTierDistribution(tenantId, programmeUid)) {
            lineConsumer.accept(join(
                "TIER_DISTRIBUTION",
                row.tierName(),
                String.valueOf(row.rankOrder()),
                String.valueOf(row.memberCount()),
                decimal(row.entryThreshold()),
                decimal(row.pointsMultiplier())
            ));
        }
    }

    public void streamSegmentAnalysis(String tenantId, String programmeUid, Consumer<String> lineConsumer) {
        for (SegmentAnalysisRow row : analyticsService.getEngagementSegments(tenantId, programmeUid)) {
            lineConsumer.accept(join(
                "ENGAGEMENT",
                row.segment(),
                String.valueOf(row.memberCount()),
                decimal(row.avgBalance()),
                decimal(row.totalPointsHeld())
            ));
        }
        for (SegmentAnalysisRow row : analyticsService.getBalanceBrackets(tenantId, programmeUid)) {
            lineConsumer.accept(join(
                "BALANCE_BRACKET",
                row.segment(),
                String.valueOf(row.memberCount()),
                decimal(row.avgBalance()),
                decimal(row.totalPointsHeld())
            ));
        }
    }

    public void streamCohortRetention(String tenantId, String programmeUid, Consumer<String> lineConsumer) {
        for (CohortRetentionRow row : analyticsService.getRetentionCohort(tenantId, programmeUid)) {
            lineConsumer.accept(join(
                row.cohortMonth(),
                String.valueOf(row.cohortSize()),
                String.valueOf(row.monthsSinceJoin()),
                String.valueOf(row.activeCustomers()),
                String.valueOf(row.retentionPct())
            ));
        }
    }

    public void streamCohortTierUpgrade(String tenantId, String programmeUid, Consumer<String> lineConsumer) {
        for (TierUpgradeCohortRow row : analyticsService.getTierUpgradeCohort(tenantId, programmeUid)) {
            lineConsumer.accept(join(
                row.cohortMonth(),
                String.valueOf(row.cohortSize()),
                String.valueOf(row.reachedSilver()),
                String.valueOf(row.silverPct()),
                row.avgDaysToSilver() == null ? "" : row.avgDaysToSilver().toString(),
                String.valueOf(row.reachedGold()),
                String.valueOf(row.goldPct()),
                row.avgDaysToGold() == null ? "" : row.avgDaysToGold().toString()
            ));
        }
    }

    public void streamCohortTierVelocity(
        String tenantId,
        String programmeUid,
        String tierName,
        Consumer<String> lineConsumer
    ) {
        for (TierVelocityBucketRow row : analyticsService.getTierVelocityBuckets(tenantId, programmeUid, tierName)) {
            lineConsumer.accept(join(tierName, row.upgradeBucket(), String.valueOf(row.memberCount())));
        }
    }

    public void streamCohortRuleEffectiveness(
        String tenantId,
        String programmeUid,
        String ruleUid,
        LocalDate from,
        LocalDate to,
        Consumer<String> lineConsumer
    ) {
        for (RuleEffectivenessRow row : analyticsService.getRuleEffectiveness(
            tenantId,
            programmeUid,
            ruleUid,
            from,
            to
        )) {
            lineConsumer.accept(join(
                ruleUid,
                row.cohort(),
                String.valueOf(row.memberCount()),
                decimal(row.totalPointsEarned()),
                String.valueOf(row.transactionCount()),
                decimal(row.avgPointsPerMember())
            ));
        }
    }

    private static String join(String... cells) {
        return String.join(",", java.util.Arrays.stream(cells).map(AnalyticsReportExportService::csvCell).toList());
    }

    private static String decimal(BigDecimal value) {
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
