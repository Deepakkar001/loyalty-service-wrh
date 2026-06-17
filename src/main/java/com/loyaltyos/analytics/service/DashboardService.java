package com.loyaltyos.analytics.service;

import com.loyaltyos.analytics.dto.CohortRetentionRow;
import com.loyaltyos.analytics.dto.DashboardEngagementSummary;
import com.loyaltyos.analytics.dto.DashboardKpiMetric;
import com.loyaltyos.analytics.dto.DashboardOverviewResponse;
import com.loyaltyos.analytics.dto.DashboardPointsEconomics;
import com.loyaltyos.analytics.dto.DashboardRetentionSummary;
import com.loyaltyos.analytics.dto.DashboardTopRuleRow;
import com.loyaltyos.analytics.dto.DashboardVolumePoint;
import com.loyaltyos.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.analytics.dto.TierDistributionRow;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.analytics.repository.DashboardQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int VOLUME_DAYS = 7;
    private static final int TOP_RULES_LIMIT = 5;
    private static final int TOP_REDEMPTIONS_LIMIT = 5;
    private static final long CACHE_TTL_MS = 45_000L;
    private static final int MAX_CACHE_ENTRIES = 512;

    private static final int MAX_RANGE_DAYS = 366;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final ConcurrentHashMap<String, CachedOverview> cache = new ConcurrentHashMap<>();

    public DashboardService(
        DashboardQueryRepository dashboardQueryRepository,
        AnalyticsQueryRepository analyticsQueryRepository
    ) {
        this.dashboardQueryRepository = Objects.requireNonNull(dashboardQueryRepository, "dashboardQueryRepository");
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(String tenantId, String programmeUid) {
        return getOverview(tenantId, programmeUid, null, null);
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(
        String tenantId,
        String programmeUid,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        String normalizedProgramme = normalizeProgramme(programmeUid);
        boolean ranged = fromDate != null && toDate != null;
        if (ranged) {
            validateDateRange(fromDate, toDate);
        }
        String cacheKey = tenantId + ":" + normalizedProgramme + ":" + (ranged ? fromDate + ":" + toDate : "default");
        long now = System.currentTimeMillis();
        CachedOverview hit = cache.get(cacheKey);
        if (hit != null && hit.expiresAtMs > now) {
            return hit.response;
        }

        DashboardOverviewResponse response = ranged
            ? buildOverviewForRange(tenantId, normalizedProgramme, fromDate, toDate)
            : buildOverview(tenantId, normalizedProgramme);
        if (cache.size() > MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        cache.put(cacheKey, new CachedOverview(response, now + CACHE_TTL_MS));
        return response;
    }

    private static void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
        if (toDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("toDate cannot be in the future");
        }
    }

    private DashboardOverviewResponse buildOverviewForRange(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();
        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(rangeDays);
        LocalDate priorTo = from.minusDays(1);
        LocalDateTime priorStart = priorFrom.atStartOfDay();
        LocalDateTime priorEnd = rangeStart;

        boolean hasData = dashboardQueryRepository.hasLedgerActivity(tenantId, programmeUid);

        long activeMembersInRange = dashboardQueryRepository.countDistinctActiveCustomers(
            tenantId, programmeUid, rangeStart, rangeEnd
        );
        long activeMembersPriorRange = dashboardQueryRepository.countDistinctActiveCustomers(
            tenantId, programmeUid, priorStart, priorEnd
        );

        BigDecimal pointsIssued = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "CREDIT", rangeStart, rangeEnd
        );
        BigDecimal pointsIssuedPrior = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "CREDIT", priorStart, priorEnd
        );

        BigDecimal redemptions = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "DEBIT", rangeStart, rangeEnd
        );
        BigDecimal redemptionsPrior = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "DEBIT", priorStart, priorEnd
        );

        BigDecimal avgOrderValue = dashboardQueryRepository
            .avgSuccessfulEventAmount(tenantId, rangeStart, rangeEnd)
            .orElse(BigDecimal.ZERO);
        BigDecimal avgOrderValuePrior = dashboardQueryRepository
            .avgSuccessfulEventAmount(tenantId, priorStart, priorEnd)
            .orElse(BigDecimal.ZERO);

        List<SegmentAnalysisRow> segments = analyticsQueryRepository.getEngagementSegmentsAsOf(
            tenantId, programmeUid, to
        );
        List<SegmentAnalysisRow> priorSegments = analyticsQueryRepository.getEngagementSegmentsAsOf(
            tenantId, programmeUid, priorTo
        );
        double atRiskPct = computeAtRiskPct(segments);
        double atRiskPriorPct = computeAtRiskPct(priorSegments);

        List<DashboardVolumePoint> volumeSeries = dashboardQueryRepository.getDailyVolumeSeries(
            tenantId, programmeUid, from, to
        );
        List<TierDistributionRow> tierDistribution = analyticsQueryRepository.getTierDistributionForActiveInRange(
            tenantId, programmeUid, from, to
        );
        List<DashboardTopRuleRow> topRules = dashboardQueryRepository.getTopRules(
            tenantId, programmeUid, from, to, TOP_RULES_LIMIT
        );
        var topRedemptions = dashboardQueryRepository.getTopRedemptions(
            tenantId, programmeUid, from, to, TOP_REDEMPTIONS_LIMIT
        );

        DashboardPointsEconomics economics = new DashboardPointsEconomics(
            pointsIssued,
            redemptions,
            pointsIssued.subtract(redemptions),
            burnRatePct(pointsIssued, redemptions)
        );

        return new DashboardOverviewResponse(
            programmeUid,
            hasData,
            kpi(activeMembersInRange, trendPct(activeMembersInRange, activeMembersPriorRange)),
            kpi(pointsIssued, trendPct(pointsIssued, pointsIssuedPrior)),
            kpi(redemptions, trendPct(redemptions, redemptionsPrior)),
            kpi(avgOrderValue, trendPct(avgOrderValue, avgOrderValuePrior)),
            kpi(atRiskPct, trendPct(atRiskPct, atRiskPriorPct)),
            volumeSeries,
            tierDistribution,
            topRules,
            topRedemptions,
            new DashboardEngagementSummary(computeActivePct(segments), segments),
            summarizeRetentionForRange(
                analyticsQueryRepository.getRetentionCohort(tenantId, programmeUid),
                from,
                to
            ),
            economics,
            Instant.now()
        );
    }

    private DashboardOverviewResponse buildOverview(String tenantId, String programmeUid) {
        LocalDate today = LocalDate.now();
        LocalDate volumeFrom = today.minusDays(VOLUME_DAYS - 1L);
        LocalDate thirtyDaysAgo = today.minusDays(29);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        LocalDateTime last7Start = today.minusDays(6).atStartOfDay();
        LocalDateTime prior7Start = today.minusDays(13).atStartOfDay();
        LocalDateTime prior7End = last7Start;

        boolean hasData = dashboardQueryRepository.hasLedgerActivity(tenantId, programmeUid);

        long activeMembers = dashboardQueryRepository.countActiveMembers(tenantId, programmeUid);
        long activeMembersPrior7d = dashboardQueryRepository.countDistinctActiveCustomers(
            tenantId, programmeUid, prior7Start, prior7End
        );
        long activeMembersLast7d = dashboardQueryRepository.countDistinctActiveCustomers(
            tenantId, programmeUid, last7Start, tomorrowStart
        );

        BigDecimal pointsIssuedToday = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "CREDIT", todayStart, tomorrowStart
        );
        BigDecimal pointsIssuedYesterday = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "CREDIT", yesterdayStart, todayStart
        );

        BigDecimal redemptionsToday = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "DEBIT", todayStart, tomorrowStart
        );
        BigDecimal redemptionsYesterday = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "DEBIT", yesterdayStart, todayStart
        );

        BigDecimal avgOrderValue = dashboardQueryRepository
            .avgSuccessfulEventAmount(tenantId, last7Start, tomorrowStart)
            .orElse(BigDecimal.ZERO);
        BigDecimal avgOrderValuePrior = dashboardQueryRepository
            .avgSuccessfulEventAmount(tenantId, prior7Start, prior7End)
            .orElse(BigDecimal.ZERO);

        List<SegmentAnalysisRow> segments = analyticsQueryRepository.getEngagementSegments(tenantId, programmeUid);
        double atRiskPct = computeAtRiskPct(segments);

        List<DashboardVolumePoint> volumeSeries = dashboardQueryRepository.getDailyVolumeSeries(
            tenantId, programmeUid, volumeFrom, today
        );
        List<TierDistributionRow> tierDistribution = analyticsQueryRepository.getTierDistribution(tenantId, programmeUid);
        List<DashboardTopRuleRow> topRules = dashboardQueryRepository.getTopRules(
            tenantId, programmeUid, volumeFrom, today, TOP_RULES_LIMIT
        );
        var topRedemptions = dashboardQueryRepository.getTopRedemptions(
            tenantId, programmeUid, volumeFrom, today, TOP_REDEMPTIONS_LIMIT
        );

        BigDecimal issued30 = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "CREDIT", thirtyDaysAgo.atStartOfDay(), tomorrowStart
        );
        BigDecimal redeemed30 = dashboardQueryRepository.sumPointsByType(
            tenantId, programmeUid, "DEBIT", thirtyDaysAgo.atStartOfDay(), tomorrowStart
        );

        DashboardPointsEconomics economics = new DashboardPointsEconomics(
            pointsIssuedToday,
            redemptionsToday,
            pointsIssuedToday.subtract(redemptionsToday),
            burnRatePct(issued30, redeemed30)
        );

        return new DashboardOverviewResponse(
            programmeUid,
            hasData,
            kpi(BigDecimal.valueOf(activeMembers), trendPct(activeMembersLast7d, activeMembersPrior7d)),
            kpi(pointsIssuedToday, trendPct(pointsIssuedToday, pointsIssuedYesterday)),
            kpi(redemptionsToday, trendPct(redemptionsToday, redemptionsYesterday)),
            kpi(avgOrderValue, trendPct(avgOrderValue, avgOrderValuePrior)),
            kpi(atRiskPct, 0.0),
            volumeSeries,
            tierDistribution,
            topRules,
            topRedemptions,
            new DashboardEngagementSummary(computeActivePct(segments), segments),
            summarizeRetention(analyticsQueryRepository.getRetentionCohort(tenantId, programmeUid)),
            economics,
            Instant.now()
        );
    }

    private static DashboardRetentionSummary summarizeRetention(List<CohortRetentionRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return new DashboardRetentionSummary(null, null);
        }
        CohortRetentionRow latest = rows.stream()
            .filter(r -> r.monthsSinceJoin() == 1)
            .max(Comparator.comparing(CohortRetentionRow::cohortMonth))
            .orElse(null);
        if (latest == null) {
            latest = rows.stream()
                .max(Comparator.comparing(CohortRetentionRow::cohortMonth)
                    .thenComparing(CohortRetentionRow::monthsSinceJoin))
                .orElse(rows.get(rows.size() - 1));
        }
        return new DashboardRetentionSummary(latest.retentionPct(), latest.cohortMonth());
    }

    private static DashboardRetentionSummary summarizeRetentionForRange(
        List<CohortRetentionRow> rows,
        LocalDate from,
        LocalDate to
    ) {
        if (rows == null || rows.isEmpty()) {
            return new DashboardRetentionSummary(null, null);
        }
        YearMonth fromMonth = YearMonth.from(from);
        YearMonth toMonth = YearMonth.from(to);
        List<CohortRetentionRow> monthOne = rows.stream()
            .filter(r -> r.monthsSinceJoin() == 1)
            .filter(r -> {
                YearMonth cohort = YearMonth.parse(r.cohortMonth());
                return !cohort.isBefore(fromMonth) && !cohort.isAfter(toMonth);
            })
            .toList();
        if (monthOne.isEmpty()) {
            return new DashboardRetentionSummary(null, null);
        }
        long totalSize = monthOne.stream().mapToLong(CohortRetentionRow::cohortSize).sum();
        if (totalSize == 0) {
            return new DashboardRetentionSummary(null, null);
        }
        double weighted = monthOne.stream()
            .mapToDouble(r -> r.retentionPct() * r.cohortSize())
            .sum();
        double pct = round1(weighted / totalSize);
        String label = monthOne.size() == 1
            ? monthOne.get(0).cohortMonth()
            : fromMonth + " – " + toMonth;
        return new DashboardRetentionSummary(pct, label);
    }

    private static double computeActivePct(List<SegmentAnalysisRow> segments) {
        if (segments == null || segments.isEmpty()) {
            return 0.0;
        }
        long total = segments.stream().mapToLong(SegmentAnalysisRow::memberCount).sum();
        if (total == 0) {
            return 0.0;
        }
        long active = segments.stream()
            .filter(s -> "ACTIVE".equalsIgnoreCase(s.segment()))
            .mapToLong(SegmentAnalysisRow::memberCount)
            .sum();
        return round1(active * 100.0 / total);
    }

    private static double computeAtRiskPct(List<SegmentAnalysisRow> segments) {
        if (segments == null || segments.isEmpty()) {
            return 0.0;
        }
        long total = segments.stream().mapToLong(SegmentAnalysisRow::memberCount).sum();
        if (total == 0) {
            return 0.0;
        }
        long atRisk = segments.stream()
            .filter(s -> "AT_RISK".equalsIgnoreCase(s.segment()) || "DORMANT".equalsIgnoreCase(s.segment()))
            .mapToLong(SegmentAnalysisRow::memberCount)
            .sum();
        return round1(atRisk * 100.0 / total);
    }

    private static BigDecimal burnRatePct(BigDecimal issued, BigDecimal redeemed) {
        if (issued == null || issued.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal r = redeemed == null ? BigDecimal.ZERO : redeemed;
        return r.multiply(new BigDecimal("100"))
            .divide(issued, 1, RoundingMode.HALF_UP);
    }

    private static DashboardKpiMetric kpi(BigDecimal value, Double trendPct) {
        return new DashboardKpiMetric(value == null ? BigDecimal.ZERO : value, trendPct);
    }

    private static DashboardKpiMetric kpi(double value, Double trendPct) {
        return new DashboardKpiMetric(BigDecimal.valueOf(value), trendPct);
    }

    private static Double trendPct(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.signum() == 0) {
            return current != null && current.signum() > 0 ? 100.0 : 0.0;
        }
        if (current == null) {
            return -100.0;
        }
        return round1(
            current.subtract(prior)
                .multiply(new BigDecimal("100"))
                .divide(prior, 4, RoundingMode.HALF_UP)
                .doubleValue()
        );
    }

    private static Double trendPct(double current, double prior) {
        if (prior == 0.0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return round1((current - prior) * 100.0 / prior);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }

    private record CachedOverview(DashboardOverviewResponse response, long expiresAtMs) {}
}
