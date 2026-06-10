package com.loyaltyos.coupon.repository;

import com.loyaltyos.coupon.dto.CouponChannelBreakdownRow;
import com.loyaltyos.coupon.dto.CouponTypeBreakdownRow;
import com.loyaltyos.coupon.dto.CouponUsageTrendRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CouponAnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CouponAnalyticsQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public long countRedemptions(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        String sql = """
            SELECT COUNT(*)
            FROM coupon_redemptions cr
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            """;
        Long count = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), Long.class);
        return count == null ? 0L : count;
    }

    public long countUniqueCustomers(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        String sql = """
            SELECT COUNT(DISTINCT cr.customer_id)
            FROM coupon_redemptions cr
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            """;
        Long count = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), Long.class);
        return count == null ? 0L : count;
    }

    public BigDecimal sumDiscount(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        return sumColumn(tenantId, programmeUid, from, to, "discount_amount");
    }

    public BigDecimal sumOrderValue(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        return sumColumn(tenantId, programmeUid, from, to, "order_amount");
    }

    public BigDecimal sumPointsCredited(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        return sumColumn(tenantId, programmeUid, from, to, "points_credited");
    }

    public List<CouponUsageTrendRow> getDailyTrend(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        String sql = """
            SELECT DATE_FORMAT(daily.day_bucket, '%Y-%m-%d') AS period,
                   daily.redemptions,
                   daily.discount_total,
                   daily.order_value_total,
                   daily.points_credited
            FROM (
              SELECT DATE(cr.redeemed_at) AS day_bucket,
                     COUNT(*) AS redemptions,
                     COALESCE(SUM(cr.discount_amount), 0) AS discount_total,
                     COALESCE(SUM(cr.order_amount), 0) AS order_value_total,
                     COALESCE(SUM(cr.points_credited), 0) AS points_credited
              FROM coupon_redemptions cr
              WHERE cr.tenant_id = :tenantId
                AND cr.programme_uid = :programmeUid
                AND cr.status = 'REDEEMED'
                AND cr.redeemed_at >= :fromDate
                AND cr.redeemed_at < :toDate
              GROUP BY DATE(cr.redeemed_at)
            ) daily
            ORDER BY daily.day_bucket ASC
            """;
        return jdbc.query(sql, rangeParams(tenantId, programmeUid, from, to), (rs, i) -> new CouponUsageTrendRow(
            rs.getString("period"),
            rs.getLong("redemptions"),
            rs.getBigDecimal("discount_total"),
            rs.getBigDecimal("order_value_total"),
            rs.getBigDecimal("points_credited")
        ));
    }

    public List<CouponChannelBreakdownRow> getChannelBreakdown(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COALESCE(NULLIF(TRIM(cr.channel), ''), 'UNKNOWN') AS channel,
                   COUNT(*) AS redemptions,
                   COALESCE(SUM(cr.discount_amount), 0) AS discount_total,
                   COALESCE(SUM(cr.order_amount), 0) AS order_value_total
            FROM coupon_redemptions cr
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            GROUP BY COALESCE(NULLIF(TRIM(cr.channel), ''), 'UNKNOWN')
            ORDER BY redemptions DESC
            """;
        return jdbc.query(sql, rangeParams(tenantId, programmeUid, from, to), (rs, i) -> new CouponChannelBreakdownRow(
            rs.getString("channel"),
            rs.getLong("redemptions"),
            rs.getBigDecimal("discount_total"),
            rs.getBigDecimal("order_value_total")
        ));
    }

    public List<CouponTypeBreakdownRow> getTypeBreakdown(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT c.coupon_type,
                   COUNT(*) AS redemptions,
                   COALESCE(SUM(cr.discount_amount), 0) AS discount_total,
                   COALESCE(SUM(cr.points_credited), 0) AS points_credited
            FROM coupon_redemptions cr
            JOIN coupons c ON c.tenant_id = cr.tenant_id AND c.coupon_uid = cr.coupon_uid
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            GROUP BY c.coupon_type
            ORDER BY redemptions DESC
            """;
        return jdbc.query(sql, rangeParams(tenantId, programmeUid, from, to), (rs, i) -> new CouponTypeBreakdownRow(
            rs.getString("coupon_type"),
            rs.getLong("redemptions"),
            rs.getBigDecimal("discount_total"),
            rs.getBigDecimal("points_credited")
        ));
    }

    public Map<String, PeriodCouponStats> getPeriodStatsByCoupon(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT cr.coupon_uid,
                   COUNT(*) AS redemptions,
                   COALESCE(SUM(cr.discount_amount), 0) AS discount_total
            FROM coupon_redemptions cr
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            GROUP BY cr.coupon_uid
            """;
        List<PeriodCouponStats> rows = jdbc.query(
            sql,
            rangeParams(tenantId, programmeUid, from, to),
            (rs, i) -> new PeriodCouponStats(
                rs.getString("coupon_uid"),
                rs.getLong("redemptions"),
                rs.getBigDecimal("discount_total")
            )
        );
        Map<String, PeriodCouponStats> map = new HashMap<>();
        for (PeriodCouponStats row : rows) {
            map.put(row.couponUid(), row);
        }
        return map;
    }

    private BigDecimal sumColumn(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        String column
    ) {
        String sql = """
            SELECT COALESCE(SUM(cr.%s), 0)
            FROM coupon_redemptions cr
            WHERE cr.tenant_id = :tenantId
              AND cr.programme_uid = :programmeUid
              AND cr.status = 'REDEEMED'
              AND cr.redeemed_at >= :fromDate
              AND cr.redeemed_at < :toDate
            """.formatted(column);
        BigDecimal sum = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private static MapSqlParameterSource rangeParams(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        return new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("programmeUid", programmeUid)
            .addValue("fromDate", from.atStartOfDay())
            .addValue("toDate", to.plusDays(1).atStartOfDay());
    }

    public record PeriodCouponStats(String couponUid, long redemptions, BigDecimal discountTotal) {}
}
