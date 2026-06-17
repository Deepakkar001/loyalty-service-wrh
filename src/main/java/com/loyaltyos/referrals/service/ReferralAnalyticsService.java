package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.dto.ReferralDashboardResponse;
import com.loyaltyos.referrals.dto.ReferralEffectivenessReportResponse;
import com.loyaltyos.referrals.dto.ReferralEffectivenessTrendRow;
import com.loyaltyos.referrals.dto.ReferralFunnelStageRow;
import com.loyaltyos.referrals.dto.ReferralProgrammeComparisonRow;
import com.loyaltyos.referrals.dto.ReferralPeriodMetrics;
import com.loyaltyos.referrals.dto.ReferralTimeToPurchaseResponse;
import com.loyaltyos.referrals.dto.ReferralTopReferrerResponse;
import com.loyaltyos.referrals.dto.ReferralTrendPointResponse;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.repository.ReferralAnalyticsQueryRepository;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.referrals.support.ReferralProgressTracker;
import com.loyaltyos.referrals.support.ReferralProgressTracker.ProgressState;
import com.loyaltyos.referrals.support.ReferralProgressTracker.PurchaseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralAnalyticsService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ReferralRepository referralRepository;
    private final ReferralAnalyticsQueryRepository analyticsQueryRepository;
    private final AnalyticsQueryRepository tenantFinanceRepository;
    private final ObjectMapper objectMapper;

    public ReferralAnalyticsService(
        ReferralRepository referralRepository,
        ReferralAnalyticsQueryRepository analyticsQueryRepository,
        AnalyticsQueryRepository tenantFinanceRepository,
        ObjectMapper objectMapper
    ) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
        this.tenantFinanceRepository = Objects.requireNonNull(tenantFinanceRepository, "tenantFinanceRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(readOnly = true)
    public ReferralEffectivenessReportResponse buildEffectivenessReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String programme = normalize(programmeUid);
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        ReferralPeriodMetrics period = analyticsQueryRepository.getPeriodMetrics(tenantId, programme, from, to);
        ReferralPeriodMetrics prior = analyticsQueryRepository.getPeriodMetrics(tenantId, programme, priorFrom, priorTo);

        TenantFinanceContext finance = tenantFinanceRepository.getTenantFinanceContext(tenantId);
        BigDecimal pointsRate = finance.pointsCurrencyRate() == null ? new BigDecimal("0.01") : finance.pointsCurrencyRate();
        BigDecimal rewardCostCurrency = period.totalRewardPoints().multiply(pointsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenuePerReward = rewardCostCurrency.signum() > 0
            ? period.totalRefereeSpend().divide(rewardCostCurrency, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal netValue = period.totalRefereeSpend().subtract(rewardCostCurrency);
        BigDecimal avgPointsPerRewarded = period.rewarded() > 0
            ? period.totalRewardPoints().divide(BigDecimal.valueOf(period.rewarded()), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        ReferralEffectivenessReportResponse report = new ReferralEffectivenessReportResponse();
        report.setProgrammeUid(programme);
        report.setFromDate(from.toString());
        report.setToDate(to.toString());
        report.setCurrency(finance.currency());
        report.setPeriodMetrics(period);
        report.setPriorPeriodMetrics(prior);
        report.setPeriodOverPeriodReferralsChangePct(percentChange(period.totalReferrals(), prior.totalReferrals()));
        report.setPeriodOverPeriodRewardedChangePct(percentChange(period.rewarded(), prior.rewarded()));
        report.setPeriodOverPeriodConversionChangePts(
            period.conversionRatePercent().subtract(prior.conversionRatePercent()).setScale(1, RoundingMode.HALF_UP)
        );
        report.setRewardCostInCurrency(rewardCostCurrency);
        report.setRevenuePerRewardCurrency(revenuePerReward);
        report.setNetRefereeValue(netValue);
        report.setAvgPointsPerRewardedReferral(avgPointsPerRewarded);
        report.setCostPerRewardedReferralPoints(avgPointsPerRewarded);
        report.setTimeToFirstPurchase(timeToFirstPurchase(tenantId, programme));
        report.setFunnel(buildFunnel(period));
        report.setDailyTrends(analyticsQueryRepository.getDailyTrends(tenantId, programme, from, to));
        report.setTopReferrers(analyticsQueryRepository.getTopReferrersInPeriod(tenantId, programme, from, to, 15));
        report.setProgrammeComparisons(analyticsQueryRepository.getProgrammeComparisons(tenantId, from, to));
        return report;
    }

    private static List<ReferralFunnelStageRow> buildFunnel(ReferralPeriodMetrics period) {
        long total = period.totalReferrals();
        List<ReferralFunnelStageRow> funnel = new ArrayList<>();
        funnel.add(stage("Pending", period.pending(), total));
        funnel.add(stage("Signed up", period.signedUp(), total));
        funnel.add(stage("Rewarded", period.rewarded(), total));
        funnel.add(stage("With purchase", period.withPurchase(), total));
        funnel.add(stage("Fraud flagged", period.fraudFlagged(), total));
        funnel.add(stage("Rejected", period.rejected(), total));
        return funnel;
    }

    private static ReferralFunnelStageRow stage(String name, long count, long total) {
        ReferralFunnelStageRow row = new ReferralFunnelStageRow();
        row.setStage(name);
        row.setCount(count);
        row.setSharePercent(total > 0
            ? BigDecimal.valueOf(count).multiply(new BigDecimal("100")).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO);
        return row;
    }

    private static BigDecimal percentChange(long current, long prior) {
        if (prior <= 0) {
            return current > 0 ? null : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public ReferralDashboardResponse dashboardForPeriod(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String programme = normalize(programmeUid);
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        ReferralPeriodMetrics period = analyticsQueryRepository.getPeriodMetrics(tenantId, programme, from, to);
        ReferralPeriodMetrics prior = analyticsQueryRepository.getPeriodMetrics(tenantId, programme, priorFrom, priorTo);

        ReferralDashboardResponse response = new ReferralDashboardResponse();
        response.setTotalReferrals(period.totalReferrals());
        response.setSignedUp(period.signedUp());
        response.setRewarded(period.rewarded());
        response.setFraudFlagged(period.fraudFlagged());
        response.setTotalPointsIssued(period.totalRewardPoints());
        if (period.totalReferrals() > 0) {
            response.setConversionRatePercent(period.conversionRatePercent().doubleValue());
            response.setAveragePointsPerReferral(
                period.totalRewardPoints()
                    .divide(BigDecimal.valueOf(period.totalReferrals()), 2, RoundingMode.HALF_UP)
            );
        }
        response.setTotalReferralsTrendPct(percentChangeDouble(period.totalReferrals(), prior.totalReferrals()));
        response.setRewardedTrendPct(percentChangeDouble(period.rewarded(), prior.rewarded()));
        response.setConversionTrendPct(
            percentChangeDouble(
                period.conversionRatePercent().doubleValue(),
                prior.conversionRatePercent().doubleValue()
            )
        );
        response.setTotalPointsTrendPct(
            percentChangeDouble(
                period.totalRewardPoints().doubleValue(),
                prior.totalRewardPoints().doubleValue()
            )
        );
        return response;
    }

    private static Double percentChangeDouble(double current, double prior) {
        if (prior == 0.0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - prior) * 100.0 / prior) * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public List<ReferralTrendPointResponse> trendsForPeriod(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String programme = normalize(programmeUid);
        return analyticsQueryRepository.getDailyTrends(tenantId, programme, from, to).stream()
            .map(row -> {
                ReferralTrendPointResponse point = new ReferralTrendPointResponse();
                point.setPeriodStart(row.getPeriodStart());
                point.setReferrals(row.getReferrals());
                point.setRewarded(row.getRewarded());
                return point;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReferralTopReferrerResponse> topReferrersForPeriod(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        String programme = normalize(programmeUid);
        int top = Math.min(Math.max(limit, 1), 50);
        return analyticsQueryRepository.getTopReferrersInPeriod(tenantId, programme, from, to, top);
    }

    @Transactional(readOnly = true)
    public List<ReferralTrendPointResponse> trends(
        String tenantId,
        String programmeUid,
        String granularity,
        int days
    ) {
        String programme = normalize(programmeUid);
        int windowDays = Math.min(Math.max(days, 1), 365);
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        boolean weekly = "WEEKLY".equalsIgnoreCase(granularity);

        List<Referral> referrals = referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme).stream()
            .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(since))
            .toList();

        Map<String, ReferralTrendPointResponse> buckets = new LinkedHashMap<>();
        for (Referral r : referrals) {
            String key = bucketKey(r.getCreatedAt(), weekly);
            ReferralTrendPointResponse point = buckets.computeIfAbsent(key, k -> {
                ReferralTrendPointResponse p = new ReferralTrendPointResponse();
                p.setPeriodStart(k);
                return p;
            });
            point.setReferrals(point.getReferrals() + 1);
            if (r.getStatus() == ReferralStatus.REWARDED) {
                point.setRewarded(point.getRewarded() + 1);
            }
        }

        List<ReferralTrendPointResponse> sorted = new ArrayList<>(buckets.values());
        sorted.sort(Comparator.comparing(ReferralTrendPointResponse::getPeriodStart));
        return sorted;
    }

    @Transactional(readOnly = true)
    public List<ReferralTopReferrerResponse> topReferrers(String tenantId, String programmeUid, int limit) {
        String programme = normalize(programmeUid);
        int top = Math.min(Math.max(limit, 1), 50);

        Map<String, ReferralTopReferrerResponse> byReferrer = new HashMap<>();
        for (Referral r : referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme)) {
            ReferralTopReferrerResponse row = byReferrer.computeIfAbsent(r.getReferrerCustomerId(), id -> {
                ReferralTopReferrerResponse t = new ReferralTopReferrerResponse();
                t.setReferrerCustomerId(id);
                return t;
            });
            row.setReferralCount(row.getReferralCount() + 1);
            if (r.getStatus() == ReferralStatus.REWARDED) {
                row.setRewardedCount(row.getRewardedCount() + 1);
            }
        }

        return byReferrer.values().stream()
            .sorted(Comparator.comparingLong(ReferralTopReferrerResponse::getReferralCount).reversed())
            .limit(top)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferralTimeToPurchaseResponse timeToFirstPurchase(String tenantId, String programmeUid) {
        String programme = normalize(programmeUid);
        ReferralTimeToPurchaseResponse resp = new ReferralTimeToPurchaseResponse();
        double totalHours = 0;
        long samples = 0;

        for (Referral r : referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme)) {
            if (r.getCreatedAt() == null) {
                continue;
            }
            ProgressState progress = ReferralProgressTracker.load(r, objectMapper);
            if (progress.getPurchases() == null || progress.getPurchases().isEmpty()) {
                if (r.getPurchaseCount() > 0 && r.getUpdatedAt() != null) {
                    totalHours += hoursBetween(r.getCreatedAt(), r.getUpdatedAt());
                    samples++;
                }
                continue;
            }
            PurchaseEvent first = progress.getPurchases().stream()
                .filter(p -> p.getAt() != null)
                .min(Comparator.comparing(PurchaseEvent::getAt))
                .orElse(null);
            if (first != null && first.getAt() != null) {
                totalHours += hoursBetween(r.getCreatedAt(), first.getAt());
                samples++;
            }
        }

        resp.setSampleSize(samples);
        resp.setAverageHoursToFirstPurchase(samples > 0 ? totalHours / samples : 0);
        return resp;
    }

    private static double hoursBetween(Instant start, Instant end) {
        return ChronoUnit.SECONDS.between(start, end) / 3600.0;
    }

    private static String bucketKey(Instant at, boolean weekly) {
        if (weekly) {
            return at.atZone(ZoneOffset.UTC).toLocalDate()
                .with(java.time.DayOfWeek.MONDAY)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return DAY_FMT.format(at);
    }

    private static String normalize(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }

    @Transactional(readOnly = true)
    public void streamEffectivenessReportCsv(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        java.util.function.Consumer<String> lineConsumer
    ) {
        ReferralEffectivenessReportResponse report = buildEffectivenessReport(tenantId, programmeUid, from, to);
        ReferralPeriodMetrics period = report.getPeriodMetrics();
        lineConsumer.accept(csvJoin(
            "SUMMARY",
            String.valueOf(period.totalReferrals()),
            String.valueOf(period.signedUp()),
            String.valueOf(period.rewarded()),
            String.valueOf(period.withPurchase()),
            decimal(period.conversionRatePercent()),
            decimal(report.getRewardCostInCurrency()),
            decimal(report.getNetRefereeValue())
        ));
        for (ReferralFunnelStageRow row : report.getFunnel()) {
            lineConsumer.accept(csvJoin(
                "FUNNEL",
                row.getStage(),
                String.valueOf(row.getCount()),
                decimal(row.getSharePercent())
            ));
        }
        for (ReferralEffectivenessTrendRow row : report.getDailyTrends()) {
            lineConsumer.accept(csvJoin(
                "DAILY",
                row.getPeriodStart(),
                String.valueOf(row.getReferrals()),
                String.valueOf(row.getRewarded()),
                decimal(row.getConversionRatePercent()),
                decimal(row.getRefereeSpend())
            ));
        }
        for (ReferralTopReferrerResponse row : report.getTopReferrers()) {
            lineConsumer.accept(csvJoin(
                "TOP_REFERRER",
                row.getReferrerCustomerId(),
                String.valueOf(row.getReferralCount()),
                String.valueOf(row.getRewardedCount()),
                decimal(row.getTotalRefereeSpend()),
                decimal(row.getPointsEarned()),
                decimal(row.getConversionRatePercent())
            ));
        }
        for (ReferralProgrammeComparisonRow row : report.getProgrammeComparisons()) {
            lineConsumer.accept(csvJoin(
                "PROGRAMME",
                row.getProgrammeUid(),
                row.getProgrammeName(),
                String.valueOf(row.getTotalReferrals()),
                decimal(row.getConversionRatePercent())
            ));
        }
    }

    private static String csvJoin(String... cells) {
        return String.join(",", java.util.Arrays.stream(cells).map(ReferralAnalyticsService::csvCell).toList());
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
