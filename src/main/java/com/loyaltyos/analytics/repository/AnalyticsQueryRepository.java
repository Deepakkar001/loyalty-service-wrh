package com.loyaltyos.analytics.repository;

import com.loyaltyos.analytics.dto.BreakageMonthlyRow;
import com.loyaltyos.analytics.dto.BreakageTierRow;
import com.loyaltyos.analytics.dto.CohortRetentionRow;
import com.loyaltyos.analytics.dto.EnrollmentRuleRow;
import com.loyaltyos.analytics.dto.EnrollmentSourceRow;
import com.loyaltyos.analytics.dto.EnrollmentSummary;
import com.loyaltyos.analytics.dto.EnrollmentTrendRow;
import com.loyaltyos.analytics.dto.ExpirePeriodSummary;
import com.loyaltyos.analytics.dto.ExpiryJobRunRow;
import com.loyaltyos.analytics.dto.PointsActivityRow;
import com.loyaltyos.analytics.dto.RuleEffectivenessRow;
import com.loyaltyos.analytics.dto.RulePerformanceRow;
import com.loyaltyos.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.analytics.dto.TenantFinanceContext;
import com.loyaltyos.analytics.dto.TierDistributionRow;
import com.loyaltyos.analytics.dto.TierUpgradeCohortRow;
import com.loyaltyos.analytics.dto.TierVelocityBucketRow;
import com.loyaltyos.analytics.dto.UpcomingExpiryMonthRow;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT DATE_FORMAT(pl.created_at, '%%Y-%%m-%%d') AS report_date,
                   pl.entry_type,
                   COUNT(*) AS transaction_count,
                   SUM(pl.points) AS total_points,
                   COUNT(DISTINCT pl.customer_id) AS unique_customers
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            GROUP BY report_date, pl.entry_type
            ORDER BY report_date ASC, pl.entry_type ASC
            """.formatted(programmeEq);
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
        String reaErProgramme = AnalyticsProgrammeSql.programmeColumnEqualsColumn(
            "rea.programme_uid",
            "er.programme_uid"
        );
        String plErProgramme = AnalyticsProgrammeSql.programmeColumnEqualsColumn(
            "pl.programme_uid",
            "er.programme_uid"
        );
        String erProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("er.programme_uid");
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
                  AND %s
                  AND JSON_CONTAINS(rea.trace_json->'$.matchedRuleUids', JSON_QUOTE(er.rule_uid))
                  AND rea.created_at >= :fromDate
                  AND rea.created_at < :toDate
            LEFT JOIN points_ledger pl
                   ON pl.source_rule_id = er.id
                  AND pl.tenant_id = er.tenant_id
                  AND %s
                  AND pl.entry_type = 'CREDIT'
                  AND pl.created_at >= :fromDate
                  AND pl.created_at < :toDate
            WHERE er.tenant_id = :tenantId
              AND %s
            GROUP BY er.rule_uid, er.name, er.status
            ORDER BY total_points_awarded DESC
            """.formatted(reaErProgramme, plErProgramme, erProgramme);
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
        String cbcTenantJoin = AnalyticsProgrammeSql.tenantColumnEquals("cbc.tenant_id", "td.tenant_id");
        String cbcProgrammeJoin = AnalyticsProgrammeSql.programmeJoin("cbc", "td");
        String sql = """
            SELECT
              td.name AS tier_name,
              td.rank_order,
              COUNT(cbc.customer_id) AS member_count,
              td.entry_threshold,
              td.points_multiplier
            FROM tier_definitions td
            LEFT JOIN customer_balance_cache cbc
                   ON %s
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
            """.formatted(cbcTenantJoin, cbcProgrammeJoin, td2Scope, tdScope);
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
        String ledgerProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String cbcProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("cbc.programme_uid");
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
                WHERE tenant_id = :tenantId AND %s
                GROUP BY customer_id
              ) last_txn ON last_txn.customer_id = cbc.customer_id
              WHERE cbc.tenant_id = :tenantId AND %s
            ) seg
            GROUP BY segment
            ORDER BY FIELD(segment, 'ACTIVE', 'AT_RISK', 'DORMANT')
            """.formatted(ledgerProgramme, cbcProgramme);
        return jdbc.query(sql, params, this::mapSegmentRow);
    }

    public List<SegmentAnalysisRow> getBalanceBrackets(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String tdScope = AnalyticsProgrammeSql.programmeScope("tier_definitions");
        String cbcProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("cbc.programme_uid");
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
            WHERE cbc.tenant_id = :tenantId AND %s
            GROUP BY balance_bracket
            ORDER BY MIN(cbc.balance)
            """.formatted(tdScope, tdScope, tdScope, tdScope, tdScope, cbcProgramme);
        return jdbc.query(sql, params, this::mapSegmentRow);
    }

    public List<CohortRetentionRow> getRetentionCohort(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String ledgerProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String plAcProgramme = AnalyticsProgrammeSql.programmeColumnEqualsColumn(
            "pl.programme_uid",
            "ac.programme_uid"
        );
        String sql = """
            WITH acquisition_cohorts AS (
              SELECT
                tenant_id,
                programme_uid,
                customer_id,
                DATE_FORMAT(MIN(created_at), '%%Y-%%m') AS cohort_month,
                MIN(created_at) AS first_txn_at
              FROM points_ledger
              WHERE tenant_id = :tenantId
                AND %s
                AND entry_type = 'CREDIT'
              GROUP BY tenant_id, programme_uid, customer_id
            ),
            monthly_activity AS (
              SELECT
                ac.cohort_month,
                TIMESTAMPDIFF(
                  MONTH,
                  STR_TO_DATE(CONCAT(ac.cohort_month, '-01'), '%%Y-%%m-%%d'),
                  pl.created_at
                ) AS months_since_join,
                COUNT(DISTINCT ac.customer_id) AS active_customers
              FROM acquisition_cohorts ac
              JOIN points_ledger pl
                ON pl.customer_id = ac.customer_id
               AND pl.tenant_id = ac.tenant_id
               AND %s
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
            """.formatted(ledgerProgramme, plAcProgramme);
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
        String ledgerProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String thProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("th.programme_uid");
        String thProgrammeBare = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String plCustomerId = AnalyticsProgrammeSql.collateColumn("customer_id");
        String thCustomerId = AnalyticsProgrammeSql.collateColumn("customer_id");
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
            member_first_activity AS (
              SELECT customer_id, MIN(first_at) AS first_txn_at
              FROM (
                SELECT %s AS customer_id, created_at AS first_at
                FROM points_ledger
                WHERE tenant_id = :tenantId AND %s AND entry_type = 'CREDIT'
                UNION ALL
                SELECT %s AS customer_id, changed_at AS first_at
                FROM tier_history
                WHERE tenant_id = :tenantId AND %s
              ) activity
              GROUP BY customer_id
            ),
            acquisition_cohorts AS (
              SELECT customer_id,
                     DATE_FORMAT(first_txn_at, '%%Y-%%m') AS cohort_month,
                     first_txn_at
              FROM member_first_activity
            ),
            first_tier_reach AS (
              SELECT th.customer_id, th.to_tier_name,
                     MIN(th.changed_at) AS reached_at
              FROM tier_history th
              WHERE th.tenant_id = :tenantId AND %s
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
              ON %s AND %s
            LEFT JOIN first_tier_reach ftr_g
              ON %s AND %s
            GROUP BY ac.cohort_month, tr2.tier_name, tr3.tier_name
            ORDER BY ac.cohort_month ASC
            """.formatted(
            tdScope,
            tdScope,
            plCustomerId,
            ledgerProgramme,
            thCustomerId,
            thProgrammeBare,
            thProgramme,
            AnalyticsProgrammeSql.programmeColumnEqualsColumn("ac.customer_id", "ftr_s.customer_id"),
            AnalyticsProgrammeSql.programmeColumnEqualsColumn("ftr_s.to_tier_name", "tr2.tier_name"),
            AnalyticsProgrammeSql.programmeColumnEqualsColumn("ac.customer_id", "ftr_g.customer_id"),
            AnalyticsProgrammeSql.programmeColumnEqualsColumn("ftr_g.to_tier_name", "tr3.tier_name")
        );
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
        String ledgerProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String thProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("th.programme_uid");
        String thProgrammeBare = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String plCustomerId = AnalyticsProgrammeSql.collateColumn("customer_id");
        String thCustomerId = AnalyticsProgrammeSql.collateColumn("customer_id");
        String sql = """
            WITH member_first_activity AS (
              SELECT customer_id, MIN(first_at) AS first_txn_at
              FROM (
                SELECT %s AS customer_id, created_at AS first_at
                FROM points_ledger
                WHERE tenant_id = :tenantId AND %s AND entry_type = 'CREDIT'
                UNION ALL
                SELECT %s AS customer_id, changed_at AS first_at
                FROM tier_history
                WHERE tenant_id = :tenantId AND %s
              ) activity
              GROUP BY customer_id
            ),
            days_to_tier AS (
              SELECT th.customer_id,
                     th.to_tier_name,
                     DATEDIFF(MIN(th.changed_at), mfa.first_txn_at) AS days_taken
              FROM tier_history th
              JOIN member_first_activity mfa ON %s
              WHERE th.tenant_id = :tenantId
               AND %s
               AND %s
              GROUP BY th.customer_id, th.to_tier_name, mfa.first_txn_at
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
            """.formatted(
            plCustomerId,
            ledgerProgramme,
            thCustomerId,
            thProgrammeBare,
            AnalyticsProgrammeSql.programmeColumnEqualsColumn("mfa.customer_id", "th.customer_id"),
            thProgramme,
            AnalyticsProgrammeSql.tierNameEqualsParam("th.to_tier_name")
        );
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
        String auditProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String cbcProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String plProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            WITH exposed AS (
              SELECT DISTINCT customer_id
              FROM rule_evaluation_audit
              WHERE tenant_id = :tenantId
                AND %s
                AND JSON_CONTAINS(trace_json->'$.matchedRuleUids', JSON_QUOTE(:ruleUid))
                AND created_at >= :fromDate
                AND created_at < :toDate
            ),
            all_customers AS (
              SELECT DISTINCT customer_id
              FROM customer_balance_cache
              WHERE tenant_id = :tenantId AND %s
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
                  AND %s
                  AND pl.entry_type = 'CREDIT'
                  AND pl.created_at >= :fromDate
                  AND pl.created_at < :toDate
            GROUP BY cohort
            ORDER BY cohort DESC
            """.formatted(auditProgramme, cbcProgramme, plProgramme);
        return jdbc.query(sql, params, (rs, i) -> new RuleEffectivenessRow(
            rs.getString("cohort"),
            rs.getLong("member_count"),
            rs.getBigDecimal("total_points_earned"),
            rs.getLong("transaction_count"),
            rs.getBigDecimal("avg_points_per_member")
        ));
    }

    public TenantFinanceContext getTenantFinanceContext(String tenantId) {
        String sql = """
            SELECT tc.points_currency_rate,
                   COALESCE(
                     (SELECT ta.points_currency
                      FROM tenant_agreements ta
                      WHERE ta.tenant_id = tc.tenant_id
                      ORDER BY ta.signed_at DESC
                      LIMIT 1),
                     'INR'
                   ) AS currency
            FROM tenant_config tc
            WHERE tc.tenant_id = :tenantId
            LIMIT 1
            """;
        var params = new MapSqlParameterSource("tenantId", tenantId);
        List<TenantFinanceContext> rows = jdbc.query(
            sql,
            params,
            (rs, i) -> new TenantFinanceContext(
                rs.getBigDecimal("points_currency_rate"),
                rs.getString("currency")
            )
        );
        if (rows.isEmpty()) {
            return new TenantFinanceContext(new BigDecimal("0.010000"), "INR");
        }
        return rows.getFirst();
    }

    public ExpirePeriodSummary getExpirePeriodSummary(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT COALESCE(SUM(pl.points), 0) AS total_expired,
                   COUNT(DISTINCT pl.customer_id) AS customers_affected,
                   COUNT(*) AS transaction_count
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = 'EXPIRE'
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            """.formatted(programmeEq);
        return jdbc.queryForObject(
            sql,
            params,
            (rs, i) -> new ExpirePeriodSummary(
                rs.getBigDecimal("total_expired"),
                rs.getLong("customers_affected"),
                rs.getLong("transaction_count")
            )
        );
    }

    public BigDecimal getOutstandingPointsLiability(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String cbcProgramme = AnalyticsProgrammeSql.programmeColumnEqualsParam("cbc.programme_uid");
        String sql = """
            SELECT COALESCE(SUM(cbc.balance), 0) AS outstanding_points
            FROM customer_balance_cache cbc
            WHERE cbc.tenant_id = :tenantId AND %s
            """.formatted(cbcProgramme);
        return Optional.ofNullable(
            jdbc.queryForObject(sql, params, (rs, i) -> rs.getBigDecimal("outstanding_points"))
        ).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getUpcomingExpiryPoints(String tenantId, String programmeUid, int withinDays) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("withinDays", withinDays);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT COALESCE(SUM(pl.points), 0) AS points_expiring
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = 'CREDIT'
              AND pl.expires_at IS NOT NULL
              AND pl.expires_at > NOW()
              AND pl.expires_at <= DATE_ADD(NOW(), INTERVAL :withinDays DAY)
              AND %s
            """.formatted(programmeEq, creditNotYetExpiredSql("pl"));
        return Optional.ofNullable(
            jdbc.queryForObject(sql, params, (rs, i) -> rs.getBigDecimal("points_expiring"))
        ).orElse(BigDecimal.ZERO);
    }

    public List<BreakageMonthlyRow> getMonthlyBreakage(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT DATE_FORMAT(pl.created_at, '%%Y-%%m') AS report_month,
                   COALESCE(SUM(pl.points), 0) AS expired_points,
                   COUNT(DISTINCT pl.customer_id) AS customers_affected,
                   COUNT(*) AS transaction_count
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = 'EXPIRE'
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            GROUP BY report_month
            ORDER BY report_month ASC
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new BreakageMonthlyRow(
            rs.getString("report_month"),
            rs.getBigDecimal("expired_points"),
            rs.getLong("customers_affected"),
            rs.getLong("transaction_count")
        ));
    }

    public List<BreakageTierRow> getBreakageByTier(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String cbcProgrammeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("cbc.programme_uid");
        String tdScope = AnalyticsProgrammeSql.programmeScope("td");
        String td2Scope = AnalyticsProgrammeSql.programmeScope("td2");
        String sql = """
            WITH expired_by_customer AS (
              SELECT pl.customer_id, SUM(pl.points) AS expired_points
              FROM points_ledger pl
              WHERE pl.tenant_id = :tenantId
                AND %s
                AND pl.entry_type = 'EXPIRE'
                AND pl.created_at >= :fromDate
                AND pl.created_at < :toDate
              GROUP BY pl.customer_id
            )
            SELECT
              COALESCE(td.name, 'Unassigned') AS tier_name,
              COALESCE(td.rank_order, 9999) AS rank_order,
              SUM(e.expired_points) AS expired_points,
              COUNT(DISTINCT e.customer_id) AS customers_affected
            FROM expired_by_customer e
            LEFT JOIN customer_balance_cache cbc
                   ON cbc.tenant_id = :tenantId
                  AND %s
                  AND cbc.customer_id = e.customer_id
            LEFT JOIN tier_definitions td
                   ON td.tenant_id = :tenantId
                  AND %s
                  AND cbc.balance >= td.entry_threshold
                  AND cbc.balance < COALESCE(
                        (SELECT MIN(td2.entry_threshold)
                         FROM tier_definitions td2
                         WHERE td2.tenant_id = td.tenant_id
                           AND %s
                           AND td2.rank_order > td.rank_order),
                        999999999)
            GROUP BY COALESCE(td.name, 'Unassigned'), COALESCE(td.rank_order, 9999)
            ORDER BY rank_order ASC
            """.formatted(programmeEq, cbcProgrammeEq, tdScope, td2Scope);
        return jdbc.query(sql, params, (rs, i) -> new BreakageTierRow(
            rs.getString("tier_name"),
            rs.getInt("rank_order"),
            rs.getBigDecimal("expired_points"),
            rs.getLong("customers_affected")
        ));
    }

    public List<UpcomingExpiryMonthRow> getUpcomingExpiryByMonth(
        String tenantId,
        String programmeUid,
        int horizonMonths
    ) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("horizonMonths", horizonMonths);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT DATE_FORMAT(pl.expires_at, '%%Y-%%m') AS expiry_month,
                   COALESCE(SUM(pl.points), 0) AS points_expiring,
                   COUNT(DISTINCT pl.customer_id) AS customers_affected
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = 'CREDIT'
              AND pl.expires_at IS NOT NULL
              AND pl.expires_at > NOW()
              AND pl.expires_at <= DATE_ADD(NOW(), INTERVAL :horizonMonths MONTH)
              AND %s
            GROUP BY expiry_month
            ORDER BY expiry_month ASC
            """.formatted(programmeEq, creditNotYetExpiredSql("pl"));
        return jdbc.query(sql, params, (rs, i) -> new UpcomingExpiryMonthRow(
            rs.getString("expiry_month"),
            rs.getBigDecimal("points_expiring"),
            rs.getLong("customers_affected")
        ));
    }

    public List<ExpiryJobRunRow> getRecentExpiryJobRuns(String tenantId, String programmeUid, int limit) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("limit", limit);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        // One row per calendar day of tenant EXPIRE ledger activity (not per platform job run).
        String sql = """
            SELECT DATE_FORMAT(daily.day_bucket, '%%Y-%%m-%%d') AS batch_date,
                   'SUCCESS' AS status,
                   daily.total_expired,
                   daily.customers_affected,
                   DATE_FORMAT(daily.last_executed_at, '%%Y-%%m-%%d %%H:%%i') AS executed_at
            FROM (
              SELECT DATE(pl.created_at) AS day_bucket,
                     SUM(pl.points) AS total_expired,
                     COUNT(DISTINCT pl.customer_id) AS customers_affected,
                     MAX(pl.created_at) AS last_executed_at
              FROM points_ledger pl
              WHERE pl.tenant_id = :tenantId
                AND %s
                AND pl.entry_type = 'EXPIRE'
              GROUP BY DATE(pl.created_at)
            ) daily
            ORDER BY daily.day_bucket DESC
            LIMIT :limit
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new ExpiryJobRunRow(
            rs.getString("batch_date"),
            rs.getString("status"),
            getNullablePointsLong(rs, "total_expired"),
            getNullableLong(rs, "customers_affected"),
            rs.getString("executed_at")
        ));
    }

    public EnrollmentSummary getEnrollmentSummary(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        LocalDate priorFrom,
        LocalDate ytdStart
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to)
            .addValue("priorFromDate", priorFrom.atStartOfDay())
            .addValue("ytdStartDate", ytdStart.atStartOfDay());
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String plProgrammeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            WITH %s
            SELECT
              (SELECT COUNT(*)
               FROM enrolled e
               WHERE e.enrolled_at >= :fromDate AND e.enrolled_at < :toDate) AS new_in_period,
              (SELECT COUNT(*)
               FROM enrolled e
               WHERE e.enrolled_at >= :priorFromDate AND e.enrolled_at < :fromDate) AS new_prior,
              (SELECT COUNT(*) FROM enrolled) AS total_enrolled,
              (SELECT COUNT(*)
               FROM enrolled e
               WHERE e.enrolled_at < :fromDate
                 AND EXISTS (
                   SELECT 1
                   FROM points_ledger pl
                   WHERE pl.tenant_id = :tenantId
                     AND %s
                     AND pl.customer_id = e.customer_id
                     AND pl.created_at >= :fromDate
                     AND pl.created_at < :toDate
                 )) AS returning_active,
              (SELECT COUNT(*)
               FROM enrolled e
               WHERE e.enrolled_at >= :ytdStartDate AND e.enrolled_at < :toDate) AS new_ytd
            """.formatted(firstCreditEnrolledCte(programmeEq), plProgrammeEq);
        return jdbc.queryForObject(
            sql,
            params,
            (rs, i) -> new EnrollmentSummary(
                rs.getLong("new_in_period"),
                rs.getLong("new_prior"),
                rs.getLong("total_enrolled"),
                rs.getLong("returning_active"),
                rs.getLong("new_ytd")
            )
        );
    }

    public List<EnrollmentTrendRow> getDailyNewEnrollments(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            WITH %s
            SELECT DATE_FORMAT(daily.day_bucket, '%%Y-%%m-%%d') AS period,
                   daily.new_enrollments
            FROM (
              SELECT DATE(e.enrolled_at) AS day_bucket,
                     COUNT(*) AS new_enrollments
              FROM enrolled e
              WHERE e.enrolled_at >= :fromDate AND e.enrolled_at < :toDate
              GROUP BY DATE(e.enrolled_at)
            ) daily
            ORDER BY daily.day_bucket ASC
            """.formatted(firstCreditEnrolledCte(programmeEq));
        return jdbc.query(sql, params, (rs, i) -> new EnrollmentTrendRow(
            rs.getString("period"),
            rs.getLong("new_enrollments")
        ));
    }

    public List<EnrollmentTrendRow> getMonthlyNewEnrollments(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            WITH %s
            SELECT monthly.period,
                   monthly.new_enrollments
            FROM (
              SELECT DATE_FORMAT(e.enrolled_at, '%%Y-%%m') AS period,
                     COUNT(*) AS new_enrollments
              FROM enrolled e
              WHERE e.enrolled_at >= :fromDate AND e.enrolled_at < :toDate
              GROUP BY DATE_FORMAT(e.enrolled_at, '%%Y-%%m')
            ) monthly
            ORDER BY monthly.period ASC
            """.formatted(firstCreditEnrolledCte(programmeEq));
        return jdbc.query(sql, params, (rs, i) -> new EnrollmentTrendRow(
            rs.getString("period"),
            rs.getLong("new_enrollments")
        ));
    }

    public List<EnrollmentSourceRow> getEnrollmentsBySource(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String referralProgrammeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("r.programme_uid");
        String sql = """
            WITH %s
            SELECT source_type,
                   COUNT(*) AS new_enrollments
            FROM (
              SELECT e.customer_id,
                     CASE
                       WHEN EXISTS (
                         SELECT 1
                         FROM referrals r
                         WHERE r.tenant_id = :tenantId
                           AND %s
                           AND r.referee_customer_id = e.customer_id
                       ) THEN 'REFERRAL'
                       WHEN e.source_campaign_id IS NOT NULL
                         AND TRIM(e.source_campaign_id) <> '' THEN 'CAMPAIGN'
                       WHEN e.source_rule_id IS NOT NULL THEN 'EARN_RULE'
                       ELSE 'DIRECT'
                     END AS source_type
              FROM enrolled e
              WHERE e.enrolled_at >= :fromDate AND e.enrolled_at < :toDate
            ) src
            GROUP BY source_type
            ORDER BY new_enrollments DESC
            """.formatted(firstCreditEnrolledCte(programmeEq), referralProgrammeEq);
        return jdbc.query(sql, params, (rs, i) -> new EnrollmentSourceRow(
            rs.getString("source_type"),
            rs.getLong("new_enrollments")
        ));
    }

    public List<EnrollmentRuleRow> getTopEnrollmentRules(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to)
            .addValue("limit", limit);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String erProgramme = AnalyticsProgrammeSql.programmeScope("er");
        String sql = """
            WITH %s
            SELECT er.rule_uid,
                   er.name AS rule_name,
                   COUNT(*) AS new_enrollments
            FROM enrolled e
            JOIN earn_rules er
              ON er.id = e.source_rule_id
             AND er.tenant_id = :tenantId
             AND %s
            WHERE e.enrolled_at >= :fromDate
              AND e.enrolled_at < :toDate
              AND e.source_rule_id IS NOT NULL
            GROUP BY er.rule_uid, er.name
            ORDER BY new_enrollments DESC
            LIMIT :limit
            """.formatted(firstCreditEnrolledCte(programmeEq), erProgramme);
        return jdbc.query(sql, params, (rs, i) -> new EnrollmentRuleRow(
            rs.getString("rule_uid"),
            rs.getString("rule_name"),
            rs.getLong("new_enrollments")
        ));
    }

    /**
     * First CREDIT row per customer — proxy for programme enrollment (no separate enrolment table).
     */
    private static String firstCreditEnrolledCte(String programmeEqPl) {
        return """
            first_credit AS (
              SELECT pl.customer_id,
                     pl.created_at AS enrolled_at,
                     pl.source_rule_id,
                     pl.source_campaign_id,
                     ROW_NUMBER() OVER (
                       PARTITION BY pl.customer_id
                       ORDER BY pl.created_at ASC, pl.id ASC
                     ) AS rn
              FROM points_ledger pl
              WHERE pl.tenant_id = :tenantId
                AND %s
                AND pl.entry_type = 'CREDIT'
            ),
            enrolled AS (
              SELECT customer_id, enrolled_at, source_rule_id, source_campaign_id
              FROM first_credit
              WHERE rn = 1
            )
            """.formatted(programmeEqPl);
    }

    /** CREDIT rows that have not yet produced an EXPIRE ledger entry. */
    private static String creditNotYetExpiredSql(String creditAlias) {
        return """
            NOT EXISTS (
              SELECT 1
              FROM points_ledger ex
              WHERE ex.tenant_id = %s.tenant_id
                AND ex.customer_id = %s.customer_id
                AND ex.idempotency_key = CONCAT('exp:', %s.id)
            )
            """.formatted(creditAlias, creditAlias, creditAlias);
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

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    private static Long getNullablePointsLong(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v == null ? null : v.longValue();
    }
}
