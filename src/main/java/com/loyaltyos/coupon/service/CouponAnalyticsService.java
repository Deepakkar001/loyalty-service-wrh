package com.loyaltyos.coupon.service;

import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.coupon.dto.CouponPerformanceRow;
import com.loyaltyos.coupon.dto.CouponUsageReportResponse;
import com.loyaltyos.coupon.dto.CouponUsageSummary;
import com.loyaltyos.coupon.entity.Coupon;
import com.loyaltyos.coupon.enums.CouponStatus;
import com.loyaltyos.coupon.repository.CouponAnalyticsQueryRepository;
import com.loyaltyos.coupon.repository.CouponAnalyticsQueryRepository.PeriodCouponStats;
import com.loyaltyos.coupon.repository.CouponRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponAnalyticsService {

    private final CouponRepository couponRepository;
    private final CouponAnalyticsQueryRepository analyticsQueryRepository;
    private final AnalyticsQueryRepository tenantFinanceRepository;

    public CouponAnalyticsService(
        CouponRepository couponRepository,
        CouponAnalyticsQueryRepository analyticsQueryRepository,
        AnalyticsQueryRepository tenantFinanceRepository
    ) {
        this.couponRepository = Objects.requireNonNull(couponRepository, "couponRepository");
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
        this.tenantFinanceRepository = Objects.requireNonNull(tenantFinanceRepository, "tenantFinanceRepository");
    }

    @Transactional(readOnly = true)
    public CouponUsageReportResponse buildUsageReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String programme = normalize(programmeUid);
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);
        LocalDate priorTo = from.minusDays(1);

        long redemptionsInPeriod = analyticsQueryRepository.countRedemptions(tenantId, programme, from, to);
        long redemptionsPrior = analyticsQueryRepository.countRedemptions(tenantId, programme, priorFrom, priorTo);

        TenantFinanceContext finance = tenantFinanceRepository.getTenantFinanceContext(tenantId);
        List<Coupon> coupons = couponRepository.findByTenantIdAndProgrammeUidOrderByCreatedAtDesc(tenantId, programme);
        Map<String, PeriodCouponStats> periodByCoupon =
            analyticsQueryRepository.getPeriodStatsByCoupon(tenantId, programme, from, to);

        CouponUsageSummary summary = new CouponUsageSummary();
        summary.setProgrammeUid(programme);
        summary.setFromDate(from.toString());
        summary.setToDate(to.toString());
        summary.setTotalCoupons(coupons.size());
        summary.setActiveCoupons(
            (int) coupons.stream().filter(c -> c.getStatus() == CouponStatus.ACTIVE).count()
        );
        summary.setRedemptionsInPeriod(redemptionsInPeriod);
        summary.setRedemptionsPriorPeriod(redemptionsPrior);
        summary.setUniqueCustomersInPeriod(analyticsQueryRepository.countUniqueCustomers(tenantId, programme, from, to));
        summary.setTotalDiscountInPeriod(analyticsQueryRepository.sumDiscount(tenantId, programme, from, to));
        summary.setTotalOrderValueInPeriod(analyticsQueryRepository.sumOrderValue(tenantId, programme, from, to));
        summary.setTotalPointsCreditedInPeriod(analyticsQueryRepository.sumPointsCredited(tenantId, programme, from, to));
        summary.setPeriodOverPeriodChangePct(percentChange(redemptionsInPeriod, redemptionsPrior));
        summary.setCurrency(finance.currency());

        List<CouponPerformanceRow> rows = new ArrayList<>();
        for (Coupon coupon : coupons) {
            PeriodCouponStats period = periodByCoupon.get(coupon.getCouponUid());
            long inPeriod = period == null ? 0L : period.redemptions();
            BigDecimal discount = period == null ? BigDecimal.ZERO : period.discountTotal();
            long allTime = coupon.getRedemptionCount();

            CouponPerformanceRow row = new CouponPerformanceRow();
            row.setCouponUid(coupon.getCouponUid());
            row.setCouponCode(coupon.getCouponCode());
            row.setCouponName(coupon.getName());
            row.setCouponType(coupon.getCouponType().name());
            row.setStatus(coupon.getStatus().name());
            row.setMaxRedemptions(coupon.getMaxRedemptions());
            row.setRedemptionsInPeriod(inPeriod);
            row.setRedemptionsAllTime(allTime);
            row.setDiscountInPeriod(discount);
            row.setUtilizationPct(utilizationPct(allTime, coupon.getMaxRedemptions()));
            row.setAvgDiscountPerRedemption(divide(discount, inPeriod));
            rows.add(row);
        }
        rows.sort(Comparator.comparingLong(CouponPerformanceRow::getRedemptionsInPeriod).reversed());

        CouponUsageReportResponse report = new CouponUsageReportResponse();
        report.setSummary(summary);
        report.setDailyRedemptions(analyticsQueryRepository.getDailyTrend(tenantId, programme, from, to));
        report.setByChannel(analyticsQueryRepository.getChannelBreakdown(tenantId, programme, from, to));
        report.setByCouponType(analyticsQueryRepository.getTypeBreakdown(tenantId, programme, from, to));
        report.setCoupons(rows);
        return report;
    }

    private static String normalize(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }

    private static BigDecimal percentChange(long current, long prior) {
        if (prior <= 0) {
            return current > 0 ? null : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal utilizationPct(long redemptions, int max) {
        if (max <= 0) {
            return null;
        }
        return BigDecimal.valueOf(redemptions)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(max), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal divide(BigDecimal numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal n = numerator == null ? BigDecimal.ZERO : numerator;
        return n.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
