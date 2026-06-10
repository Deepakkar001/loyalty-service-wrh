package com.loyaltyos.referrals.repository;

import com.loyaltyos.referrals.dto.ReferralEffectivenessTrendRow;
import com.loyaltyos.referrals.dto.ReferralPeriodMetrics;
import com.loyaltyos.referrals.dto.ReferralProgrammeComparisonRow;
import com.loyaltyos.referrals.dto.ReferralTopReferrerResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReferralAnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReferralAnalyticsQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public ReferralPeriodMetrics getPeriodMetrics(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COUNT(*) AS total_referrals,
                   SUM(CASE WHEN status IN ('SIGNED_UP','REWARDED') THEN 1 ELSE 0 END) AS signed_up,
                   SUM(CASE WHEN status = 'REWARDED' THEN 1 ELSE 0 END) AS rewarded,
                   SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
                   SUM(CASE WHEN status = 'FRAUD_FLAGGED' THEN 1 ELSE 0 END) AS fraud_flagged,
                   SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected,
                   SUM(CASE WHEN purchase_count > 0 THEN 1 ELSE 0 END) AS with_purchase,
                   COALESCE(SUM(total_spend), 0) AS total_referee_spend
            FROM referrals
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND created_at >= :fromDate
              AND created_at < :toDate
            """;
        return jdbc.queryForObject(
            sql,
            rangeParams(tenantId, programmeUid, from, to),
            (rs, i) -> toPeriodMetrics(rs.getLong("total_referrals"), rs.getLong("signed_up"), rs.getLong("rewarded"),
                rs.getLong("pending"), rs.getLong("fraud_flagged"), rs.getLong("rejected"), rs.getLong("with_purchase"),
                rs.getBigDecimal("total_referee_spend"), sumRewardPoints(tenantId, programmeUid, from, to))
        );
    }

    public BigDecimal sumRewardPoints(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        String sql = """
            SELECT COALESCE(SUM(points_awarded), 0)
            FROM referral_rewards_issued
            WHERE tenant_id = :tenantId
              AND programme_uid = :programmeUid
              AND created_at >= :fromDate
              AND created_at < :toDate
            """;
        BigDecimal sum = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public List<ReferralEffectivenessTrendRow> getDailyTrends(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT DATE_FORMAT(daily.day_bucket, '%Y-%m-%d') AS period_start,
                   daily.referrals,
                   daily.signed_up,
                   daily.rewarded,
                   daily.referee_spend
            FROM (
              SELECT DATE(created_at) AS day_bucket,
                     COUNT(*) AS referrals,
                     SUM(CASE WHEN status IN ('SIGNED_UP','REWARDED') THEN 1 ELSE 0 END) AS signed_up,
                     SUM(CASE WHEN status = 'REWARDED' THEN 1 ELSE 0 END) AS rewarded,
                     COALESCE(SUM(total_spend), 0) AS referee_spend
              FROM referrals
              WHERE tenant_id = :tenantId
                AND programme_uid = :programmeUid
                AND created_at >= :fromDate
                AND created_at < :toDate
              GROUP BY DATE(created_at)
            ) daily
            ORDER BY daily.day_bucket ASC
            """;
        return jdbc.query(sql, rangeParams(tenantId, programmeUid, from, to), (rs, i) -> {
            ReferralEffectivenessTrendRow row = new ReferralEffectivenessTrendRow();
            row.setPeriodStart(rs.getString("period_start"));
            long referrals = rs.getLong("referrals");
            long rewarded = rs.getLong("rewarded");
            row.setReferrals(referrals);
            row.setSignedUp(rs.getLong("signed_up"));
            row.setRewarded(rewarded);
            row.setRefereeSpend(rs.getBigDecimal("referee_spend"));
            row.setConversionRatePercent(rate(rewarded, referrals));
            return row;
        });
    }

    public List<ReferralTopReferrerResponse> getTopReferrersInPeriod(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        String sql = """
            SELECT r.referrer_customer_id,
                   COUNT(*) AS referral_count,
                   SUM(CASE WHEN r.status = 'REWARDED' THEN 1 ELSE 0 END) AS rewarded_count,
                   COALESCE(SUM(r.total_spend), 0) AS total_referee_spend,
                   COALESCE(SUM(ri.points_awarded), 0) AS points_earned
            FROM referrals r
            LEFT JOIN referral_rewards_issued ri
              ON ri.tenant_id = r.tenant_id
             AND ri.referral_uid = r.referral_uid
             AND ri.recipient_type = 'REFERRER'
            WHERE r.tenant_id = :tenantId
              AND r.programme_uid = :programmeUid
              AND r.created_at >= :fromDate
              AND r.created_at < :toDate
            GROUP BY r.referrer_customer_id
            ORDER BY referral_count DESC
            LIMIT :limit
            """;
        return jdbc.query(
            sql,
            rangeParams(tenantId, programmeUid, from, to).addValue("limit", limit),
            (rs, i) -> {
                ReferralTopReferrerResponse row = new ReferralTopReferrerResponse();
                long count = rs.getLong("referral_count");
                long rewarded = rs.getLong("rewarded_count");
                row.setReferrerCustomerId(rs.getString("referrer_customer_id"));
                row.setReferralCount(count);
                row.setRewardedCount(rewarded);
                row.setTotalRefereeSpend(rs.getBigDecimal("total_referee_spend"));
                row.setPointsEarned(rs.getBigDecimal("points_earned"));
                row.setConversionRatePercent(rate(rewarded, count));
                return row;
            }
        );
    }

    public List<ReferralProgrammeComparisonRow> getProgrammeComparisons(
        String tenantId,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT stats.programme_uid,
                   COALESCE(rp.name, stats.programme_uid) AS programme_name,
                   stats.total_referrals,
                   stats.rewarded,
                   stats.total_referee_spend,
                   COALESCE(pts.total_reward_points, 0) AS total_reward_points
            FROM (
              SELECT programme_uid,
                     COUNT(*) AS total_referrals,
                     SUM(CASE WHEN status = 'REWARDED' THEN 1 ELSE 0 END) AS rewarded,
                     COALESCE(SUM(total_spend), 0) AS total_referee_spend
              FROM referrals
              WHERE tenant_id = :tenantId
                AND created_at >= :fromDate
                AND created_at < :toDate
              GROUP BY programme_uid
            ) stats
            LEFT JOIN referral_programmes rp
              ON rp.tenant_id = :tenantId
             AND rp.programme_uid = stats.programme_uid
            LEFT JOIN (
              SELECT programme_uid, SUM(points_awarded) AS total_reward_points
              FROM referral_rewards_issued
              WHERE tenant_id = :tenantId
                AND created_at >= :fromDate
                AND created_at < :toDate
              GROUP BY programme_uid
            ) pts ON pts.programme_uid = stats.programme_uid
            ORDER BY stats.total_referrals DESC
            """;
        var params = new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("fromDate", from.atStartOfDay())
            .addValue("toDate", to.plusDays(1).atStartOfDay());
        return jdbc.query(sql, params, (rs, i) -> {
            ReferralProgrammeComparisonRow row = new ReferralProgrammeComparisonRow();
            long total = rs.getLong("total_referrals");
            long rewarded = rs.getLong("rewarded");
            row.setProgrammeUid(rs.getString("programme_uid"));
            row.setProgrammeName(rs.getString("programme_name"));
            row.setTotalReferrals(total);
            row.setRewarded(rewarded);
            row.setConversionRatePercent(rate(rewarded, total));
            row.setTotalRefereeSpend(rs.getBigDecimal("total_referee_spend"));
            row.setTotalRewardPoints(rs.getBigDecimal("total_reward_points"));
            return row;
        });
    }

    private static ReferralPeriodMetrics toPeriodMetrics(
        long total,
        long signedUp,
        long rewarded,
        long pending,
        long fraudFlagged,
        long rejected,
        long withPurchase,
        BigDecimal spend,
        BigDecimal rewardPoints
    ) {
        return new ReferralPeriodMetrics(
            total,
            signedUp,
            rewarded,
            pending,
            fraudFlagged,
            rejected,
            withPurchase,
            spend == null ? BigDecimal.ZERO : spend,
            rewardPoints == null ? BigDecimal.ZERO : rewardPoints,
            rate(rewarded, total),
            rate(signedUp, total),
            rate(withPurchase, total)
        );
    }

    private static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
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
}
