package com.loyaltyos.onboarding.analytics.repository;

import com.loyaltyos.onboarding.analytics.dto.CohortRetentionRow;
import com.loyaltyos.onboarding.analytics.dto.PointsActivityRow;
import com.loyaltyos.onboarding.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.onboarding.analytics.dto.RulePerformanceRow;
import com.loyaltyos.onboarding.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.onboarding.analytics.dto.TierDistributionRow;
import com.loyaltyos.onboarding.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.onboarding.analytics.dto.TierVelocityBucketRow;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JDBC-backed analytics queries. Not a Spring Data repository — wired as a plain {@link Component}.
 */
@Component
public class AnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public List<PointsActivityRow> getPointsActivity(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String sql = """
            SELECT DATE_FORMAT(pl.created_at, '%Y-%m-%d') AS report_date,
                   pl.entry_type,
                   COUNT(*) AS transaction_count,
                   SUM(pl.points) AS total_points,
                   COUNT(DISTINCT pl.customer_id) AS unique_customers
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND pl.programme_uid = :programmeUid
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            GROUP BY report_date, pl.entry_type
            ORDER BY report_date ASC, pl.entry_type ASC
            """;
        return jdbc.query(sql, params, (rs, i) -> new PointsActivityRow(
            rs.getString("report_date"),
            rs.getString("entry_type"),
            rs.getLong("transaction_count"),
            rs.getBigDecimal("total_points"),
            rs.getLong("unique_customers")
        ));
    }

    public List<RulePerformanceRow> getRulePerformance(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String sql = """
            SELECT
              er.rule_uid,
              er.name AS rule_name,
              er.status,
              COUNT(DISTINCT rea.id) AS evaluation_count,
              SUM(CASE WHEN rea.success = 1 THEN 1 ELSE 0 END) AS success_count,
              COALESCE(SUM(pl.points), 0) AS total_points_awarded
            FROM earn_rules er
            LEFT JOIN rule_evaluation_audit rea
                   ON rea.tenant_id = er.tenant_id
                  AND rea.programme_uid = er.programme_uid
                  AND JSON_CONTAINS(rea.trace_json->'$.matchedRuleUids', JSON_QUOTE(er.rule_uid))
                  AND rea.created_at >= :fromDate
                  AND rea.created_at < :toDate
            LEFT JOIN points_ledger pl
                   ON pl.source_rule_id = er.id
                  AND pl.tenant_id = er.tenant_id
                  AND pl.programme_uid = er.programme_uid
                  AND pl.entry_type = 'CREDIT'
                  AND pl.created_at >= :fromDate
                  AND pl.created_at < :toDate
            WHERE er.tenant_id = :tenantId
              AND er.programme_uid = :programmeUid
            GROUP BY er.rule_uid, er.name, er.status
            ORDER BY total_points_awarded DESC
            """;
        return jdbc.query(sql, params, (rs, i) -> new RulePerformanceRow(
            rs.getString("rule_uid"),
            rs.getString("rule_name"),
            rs.getString("status"),
            rs.getLong("evaluation_count"),
            rs.getLong("success_count"),
            rs.getBigDecimal("total_points_awarded")
        ));
    }

    public List<TierDistributionRow> getTierDistribution(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String tdScope = AnalyticsProgrammeSql.programmeScope("td");
        String td2Scope = AnalyticsProgrammeSql.programmeScope("td2");
        String cbcJoin = AnalyticsProgrammeSql.programmeJoin("cbc", "td");
        String sql = """
            SELECT
              td.name AS tier_name,
              td.rank_order,
              COUNT(cbc.customer_id) AS member_count,
              td.entry_threshold,
              td.points_multiplier
            FROM tier_definitions td
            LEFT JOIN customer_balance_cache cbc
                   ON cbc.tenant_id = td.tenant_id
                  AND %s
                  AND cbc.balance >= td.entry_threshold
                  AND cbc.balance < COALESCE(
                        (SELECT MIN(td2.entry_threshold)
                         FROM tier_definitions td2
                         WHERE td2.tenant_id = td.tenant_id
                           AND %s
                           AND td2.rank_order > td.rank_order),
                        999999999)
            WHERE td.tenant_id = :tenantId
              AND %s
            GROUP BY td.name, td.rank_order, td.entry_threshold, td.points_multiplier
            ORDER BY td.rank_order ASC
            """.formatted(cbcJoin, td2Scope, tdScope);
        return jdbc.query(sql, params, (rs, i) -> new TierDistributionRow(
            rs.getString("tier_name"),
            rs.getInt("rank_order"),
            rs.getLong("member_count"),
            rs.getBigDecimal("entry_threshold"),
            rs.getBigDecimal("points_multiplier")
        ));
    }

    public List<SegmentAnalysisRow> getEngagementSegments(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String sql = """
            SELECT segment,
                   COUNT(*) AS member_count,
                   ROUND(AVG(balance), 2) AS avg_balance,
                   ROUND(SUM(balance), 2) AS total_points_held
            FROM (
              SELECT
                cbc.customer_id,
                cbc.balance,
                CASE
                  WHEN DATEDIFF(NOW(), COALESCE(last_txn.last_txn_date, '1970-01-01')) <= 30 THEN 'ACTIVE'
                  WHEN DATEDIFF(NOW(), COALESCE(last_txn.last_txn_date, '1970-01-01')) <= 90 THEN 'AT_RISK'
                  ELSE 'DORMANT'
                END AS segment
              FROM customer_balance_cache cbc
              LEFT JOIN (
                SELECT customer_id, MAX(created_at) AS last_txn_date
                FROM points_ledger
                WHERE tenant_id = :tenantId AND programme_uid = :programmeUid
                GROUP BY customer_id
              ) last_txn ON last_txn.customer_id = cbc.customer_id
              WHERE cbc.tenant_id = :tenantId AND cbc.programme_uid = :programmeUid
            ) seg
            GROUP BY segment
            ORDER BY FIELD(segment, 'ACTIVE', 'AT_RISK', 'DORMANT')
            """;
        return jdbc.query(sql, params, this::mapSegmentRow);
    }

    public List<SegmentAnalysisRow> getBalanceBrackets(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String tdScope = AnalyticsProgrammeSql.programmeScope("tier_definitions");
        String sql = """
            SELECT
              CASE
                WHEN cbc.balance = 0 THEN 'Zero balance'
                WHEN cbc.balance < COALESCE((
                  SELECT MIN(entry_threshold)
                  FROM tier_definitions
                  WHERE tenant_id = :tenantId AND %s AND rank_order = 2
                ), 999999999) THEN CONCAT('Below ', COALESCE((
                  SELECT MIN(name)
                  FROM tier_definitions
                  WHERE tenant_id = :tenantId AND %s AND rank_order = 2
                ), 'mid tier'))
                WHEN cbc.balance < COALESCE((
                  SELECT MIN(entry_threshold)
                  FROM tier_definitions
                  WHERE tenant_id = :tenantId AND %s AND rank_order = 3
                ), 999999999) THEN CONCAT(COALESCE((
                  SELECT MIN(name)
                  FROM tier_definitions
                  WHERE tenant_id = :tenantId AND %s AND rank_order = 2
                ), 'Lower'), ' range')
                ELSE CONCAT(COALESCE((
                  SELECT MIN(name)
                  FROM tier_definitions
                  WHERE tenant_id = :tenantId AND %s AND rank_order = 3
                ), 'Top'), '+ range')
              END AS balance_bracket,
              COUNT(*) AS member_count,
              ROUND(AVG(cbc.balance), 2) AS avg_balance,
              ROUND(SUM(cbc.balance), 2) AS total_points_held
            FROM customer_balance_cache cbc
            WHERE cbc.tenant_id = :tenantId AND cbc.programme_uid = :programmeUid
            GROUP BY balance_bracket
            ORDER BY MIN(cbc.balance)
            """.formatted(tdScope, tdScope, tdScope, tdScope, tdScope);
        return jdbc.query(sql, params, this::mapSegmentRow);
    }

    public List<CohortRetentionRow> getRetentionCohort(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String sql = """
            WITH acquisition_cohorts AS (
              SELECT
                tenant_id,
                programme_uid,
                customer_id,
                DATE_FORMAT(MIN(created_at), '%Y-%m') AS cohort_month,
                MIN(created_at) AS first_txn_at
              FROM points_ledger
              WHERE tenant_id = :tenantId
                AND programme_uid = :programmeUid
                AND entry_type = 'CREDIT'
              GROUP BY tenant_id, programme_uid, customer_id
            ),
            monthly_activity AS (
              SELECT
                ac.cohort_month,
                TIMESTAMPDIFF(
                  MONTH,
                  STR_TO_DATE(CONCAT(ac.cohort_month, '-01'), '%Y-%m-%d'),
                  pl.created_at
                ) AS months_since_join,
                COUNT(DISTINCT ac.customer_id) AS active_customers
              FROM acquisition_cohorts ac
              JOIN points_ledger pl
                ON pl.customer_id = ac.customer_id
               AND pl.tenant_id = ac.tenant_id
               AND pl.programme_uid = ac.programme_uid
               AND pl.entry_type = 'CREDIT'
              GROUP BY ac.cohort_month, months_since_join
            ),
            cohort_sizes AS (
              SELECT cohort_month, COUNT(*) AS cohort_size
              FROM acquisition_cohorts
              GROUP BY cohort_month
            )
            SELECT
              ma.cohort_month,
              cs.cohort_size,
              ma.months_since_join,
              ma.active_customers,
              ROUND(ma.active_customers / cs.cohort_size * 100, 1) AS retention_pct
            FROM monthly_activity ma
            JOIN cohort_sizes cs ON cs.cohort_month = ma.cohort_month
            ORDER BY ma.cohort_month ASC, ma.months_since_join ASC
            """;
        return jdbc.query(sql, params, (rs, i) -> new CohortRetentionRow(
            rs.getString("cohort_month"),
            rs.getLong("cohort_size"),
            rs.getInt("months_since_join"),
            rs.getLong("active_customers"),
            rs.getDouble("retention_pct")
        ));
    }

    public List<TierUpgradeCohortRow> getTierUpgradeCohort(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String tdScope = AnalyticsProgrammeSql.programmeScope("tier_definitions");
        String sql = """
            WITH tier_rank_2 AS (
              SELECT MIN(name) AS tier_name
              FROM tier_definitions
              WHERE tenant_id = :tenantId AND %s AND rank_order = 2
            ),
            tier_rank_3 AS (
              SELECT MIN(name) AS tier_name
              FROM tier_definitions
              WHERE tenant_id = :tenantId AND %s AND rank_order = 3
            ),
            acquisition_cohorts AS (
              SELECT customer_id,
                     DATE_FORMAT(MIN(created_at), '%%Y-%%m') AS cohort_month,
                     MIN(created_at) AS first_txn_at
              FROM points_ledger
              WHERE tenant_id = :tenantId AND programme_uid = :programmeUid AND entry_type = 'CREDIT'
              GROUP BY customer_id
            ),
            first_tier_reach AS (
              SELECT th.customer_id, th.to_tier_name,
                     MIN(th.changed_at) AS reached_at
              FROM tier_history th
              WHERE th.tenant_id = :tenantId AND th.programme_uid = :programmeUid
              GROUP BY th.customer_id, th.to_tier_name
            )
            SELECT
              ac.cohort_month,
              COUNT(DISTINCT ac.customer_id) AS cohort_size,
              COUNT(DISTINCT CASE WHEN ftr_s.to_tier_name = tr2.tier_name THEN ac.customer_id END) AS reached_silver,
              ROUND(
                COUNT(DISTINCT CASE WHEN ftr_s.to_tier_name = tr2.tier_name THEN ac.customer_id END)
                / NULLIF(COUNT(DISTINCT ac.customer_id), 0) * 100, 1
              ) AS silver_pct,
              ROUND(AVG(CASE WHEN ftr_s.to_tier_name = tr2.tier_name
                THEN DATEDIFF(ftr_s.reached_at, ac.first_txn_at) END), 1) AS avg_days_to_silver,
              COUNT(DISTINCT CASE WHEN ftr_g.to_tier_name = tr3.tier_name THEN ac.customer_id END) AS reached_gold,
              ROUND(
                COUNT(DISTINCT CASE WHEN ftr_g.to_tier_name = tr3.tier_name THEN ac.customer_id END)
                / NULLIF(COUNT(DISTINCT ac.customer_id), 0) * 100, 1
              ) AS gold_pct,
              ROUND(AVG(CASE WHEN ftr_g.to_tier_name = tr3.tier_name
                THEN DATEDIFF(ftr_g.reached_at, ac.first_txn_at) END), 1) AS avg_days_to_gold
            FROM acquisition_cohorts ac
            CROSS JOIN tier_rank_2 tr2
            CROSS JOIN tier_rank_3 tr3
            LEFT JOIN first_tier_reach ftr_s
              ON ac.customer_id = ftr_s.customer_id AND ftr_s.to_tier_name = tr2.tier_name
            LEFT JOIN first_tier_reach ftr_g
              ON ac.customer_id = ftr_g.customer_id AND ftr_g.to_tier_name = tr3.tier_name
            GROUP BY ac.cohort_month, tr2.tier_name, tr3.tier_name
            ORDER BY ac.cohort_month ASC
            """.formatted(tdScope, tdScope);
        return jdbc.query(sql, params, (rs, i) -> new TierUpgradeCohortRow(
            rs.getString("cohort_month"),
            rs.getLong("cohort_size"),
            rs.getLong("reached_silver"),
            nullToZero(rs.getObject("silver_pct")),
            getNullableDouble(rs, "avg_days_to_silver"),
            rs.getLong("reached_gold"),
            nullToZero(rs.getObject("gold_pct")),
            getNullableDouble(rs, "avg_days_to_gold")
        ));
    }

    public List<TierVelocityBucketRow> getTierVelocityBuckets(
        String tenantId,
        String programmeUid,
        String tierName
    ) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("tierName", tierName);
        String sql = """
            WITH acquisition_cohorts AS (
              SELECT customer_id, MIN(created_at) AS first_txn_at
              FROM points_ledger
              WHERE tenant_id = :tenantId AND programme_uid = :programmeUid AND entry_type = 'CREDIT'
              GROUP BY customer_id
            ),
            days_to_tier AS (
              SELECT ac.customer_id,
                     th.to_tier_name,
                     DATEDIFF(MIN(th.changed_at), ac.first_txn_at) AS days_taken
              FROM acquisition_cohorts ac
              JOIN tier_history th
                ON ac.customer_id = th.customer_id
               AND th.tenant_id = :tenantId
               AND th.programme_uid = :programmeUid
               AND th.to_tier_name = :tierName
              GROUP BY ac.customer_id, th.to_tier_name, ac.first_txn_at
            )
            SELECT
              CASE
                WHEN days_taken <= 7 THEN '0-7 days'
                WHEN days_taken <= 14 THEN '8-14 days'
                WHEN days_taken <= 30 THEN '15-30 days'
                WHEN days_taken <= 60 THEN '31-60 days'
                ELSE '60+ days'
              END AS upgrade_bucket,
              COUNT(*) AS member_count
            FROM days_to_tier
            GROUP BY upgrade_bucket
            ORDER BY MIN(days_taken)
            """;
        return jdbc.query(sql, params, (rs, i) -> new TierVelocityBucketRow(
            rs.getString("upgrade_bucket"),
            rs.getLong("member_count")
        ));
    }

    public List<RuleEffectivenessRow> getRuleEffectiveness(
        String tenantId,
        String programmeUid,
        String ruleUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to)
            .addValue("ruleUid", ruleUid);
        String sql = """
            WITH exposed AS (
              SELECT DISTINCT customer_id
              FROM rule_evaluation_audit
              WHERE tenant_id = :tenantId
                AND programme_uid = :programmeUid
                AND JSON_CONTAINS(trace_json->'$.matchedRuleUids', JSON_QUOTE(:ruleUid))
                AND created_at >= :fromDate
                AND created_at < :toDate
            ),
            all_customers AS (
              SELECT DISTINCT customer_id
              FROM customer_balance_cache
              WHERE tenant_id = :tenantId AND programme_uid = :programmeUid
            )
            SELECT
              CASE WHEN ac.customer_id IN (SELECT customer_id FROM exposed)
                   THEN 'EXPOSED' ELSE 'NOT_EXPOSED' END AS cohort,
              COUNT(DISTINCT ac.customer_id) AS member_count,
              COALESCE(SUM(pl.points), 0) AS total_points_earned,
              COUNT(pl.id) AS transaction_count,
              ROUND(COALESCE(SUM(pl.points), 0) / NULLIF(COUNT(DISTINCT ac.customer_id), 0), 2) AS avg_points_per_member
            FROM all_customers ac
            LEFT JOIN points_ledger pl
                   ON pl.customer_id = ac.customer_id
                  AND pl.tenant_id = :tenantId
                  AND pl.programme_uid = :programmeUid
                  AND pl.entry_type = 'CREDIT'
                  AND pl.created_at >= :fromDate
                  AND pl.created_at < :toDate
            GROUP BY cohort
            ORDER BY cohort DESC
            """;
        return jdbc.query(sql, params, (rs, i) -> new RuleEffectivenessRow(
            rs.getString("cohort"),
            rs.getLong("member_count"),
            rs.getBigDecimal("total_points_earned"),
            rs.getLong("transaction_count"),
            rs.getBigDecimal("avg_points_per_member")
        ));
    }

    private SegmentAnalysisRow mapSegmentRow(ResultSet rs, int rowNum) throws SQLException {
        String label = rs.getString("segment");
        if (label == null) {
            label = rs.getString("balance_bracket");
        }
        return new SegmentAnalysisRow(
            label,
            rs.getLong("member_count"),
            rs.getBigDecimal("avg_balance"),
            rs.getBigDecimal("total_points_held")
        );
    }

    private static MapSqlParameterSource tenantProgrammeParams(String tenantId, String programmeUid) {
        return new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("programmeUid", programmeUid);
    }

    private static MapSqlParameterSource baseRangeParams(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        return tenantProgrammeParams(tenantId, programmeUid)
            .addValue("fromDate", from.atStartOfDay())
            .addValue("toDate", to.plusDays(1).atStartOfDay());
    }

    private static double nullToZero(Object v) {
        if (v == null) {
            return 0.0;
        }
        if (v instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }
}
