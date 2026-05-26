package com.loyaltyos.analytics.repository;

import com.loyaltyos.analytics.dto.DashboardRedemptionRow;
import com.loyaltyos.analytics.dto.DashboardTopRuleRow;
import com.loyaltyos.analytics.dto.DashboardVolumePoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Focused read queries for the tenant dashboard overview. Keeps aggregations narrow and indexed.
 */
@Component
public class DashboardQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public boolean hasLedgerActivity(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT 1
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND %s
            LIMIT 1
            """.formatted(programmeEq);
        List<Integer> rows = jdbc.query(sql, params, (rs, i) -> 1);
        return !rows.isEmpty();
    }

    public long countActiveMembers(String tenantId, String programmeUid) {
        var params = tenantProgrammeParams(tenantId, programmeUid);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT COUNT(*) AS cnt
            FROM customer_balance_cache
            WHERE tenant_id = :tenantId
              AND %s
            """.formatted(programmeEq);
        Long cnt = jdbc.queryForObject(sql, params, Long.class);
        return cnt == null ? 0L : cnt;
    }

    public long countDistinctActiveCustomers(
        String tenantId,
        String programmeUid,
        LocalDateTime from,
        LocalDateTime to
    ) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("fromDate", from)
            .addValue("toDate", to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT COUNT(DISTINCT customer_id) AS cnt
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND %s
              AND created_at >= :fromDate
              AND created_at < :toDate
            """.formatted(programmeEq);
        Long cnt = jdbc.queryForObject(sql, params, Long.class);
        return cnt == null ? 0L : cnt;
    }

    public BigDecimal sumPointsByType(
        String tenantId,
        String programmeUid,
        String entryType,
        LocalDateTime from,
        LocalDateTime to
    ) {
        var params = tenantProgrammeParams(tenantId, programmeUid)
            .addValue("entryType", entryType)
            .addValue("fromDate", from)
            .addValue("toDate", to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT COALESCE(SUM(points), 0) AS total
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND %s
              AND entry_type = :entryType
              AND created_at >= :fromDate
              AND created_at < :toDate
            """.formatted(programmeEq);
        BigDecimal total = jdbc.queryForObject(sql, params, BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    public Optional<BigDecimal> avgSuccessfulEventAmount(
        String tenantId,
        LocalDateTime from,
        LocalDateTime to
    ) {
        var params = new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("fromDate", from)
            .addValue("toDate", to);
        String sql = """
            SELECT ROUND(AVG(amount), 2) AS avg_amount
            FROM integration_event_processing_log
            WHERE tenant_id = :tenantId
              AND processing_status = 'SUCCESS'
              AND created_at >= :fromDate
              AND created_at < :toDate
            """;
        BigDecimal avg = jdbc.queryForObject(sql, params, BigDecimal.class);
        return Optional.ofNullable(avg);
    }

    public List<DashboardVolumePoint> getDailyVolumeSeries(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT DATE_FORMAT(created_at, '%%Y-%%m-%%d') AS report_date,
                   COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN points ELSE 0 END), 0) AS issued,
                   COALESCE(SUM(CASE WHEN entry_type = 'DEBIT' THEN points ELSE 0 END), 0) AS redeemed
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND %s
              AND created_at >= :fromDate
              AND created_at < :toDate
            GROUP BY report_date
            ORDER BY report_date ASC
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new DashboardVolumePoint(
            rs.getString("report_date"),
            rs.getBigDecimal("issued"),
            rs.getBigDecimal("redeemed")
        ));
    }

    public List<DashboardTopRuleRow> getTopRules(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to)
            .addValue("limit", Math.min(Math.max(limit, 1), 20));
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
              COUNT(DISTINCT rea.id) AS evaluation_count,
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
            GROUP BY er.rule_uid, er.name
            ORDER BY total_points_awarded DESC, evaluation_count DESC
            LIMIT :limit
            """.formatted(reaErProgramme, plErProgramme, erProgramme);
        return jdbc.query(sql, params, (rs, i) -> new DashboardTopRuleRow(
            rs.getString("rule_uid"),
            rs.getString("rule_name"),
            rs.getLong("evaluation_count"),
            rs.getBigDecimal("total_points_awarded")
        ));
    }

    public List<DashboardRedemptionRow> getTopRedemptions(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = baseRangeParams(tenantId, programmeUid, from, to)
            .addValue("limit", Math.min(Math.max(limit, 1), 20));
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("programme_uid");
        String sql = """
            SELECT
              CASE
                WHEN description IS NULL OR description = '' THEN 'Redemption'
                WHEN description LIKE 'REDEMPTION %% catalog=%%' THEN
                  SUBSTRING_INDEX(SUBSTRING_INDEX(description, 'catalog=', -1), ' ', 1)
                ELSE LEFT(description, 80)
              END AS reward_label,
              COUNT(*) AS redemption_count,
              COALESCE(SUM(points), 0) AS total_points
            FROM points_ledger
            WHERE tenant_id = :tenantId
              AND %s
              AND entry_type = 'DEBIT'
              AND created_at >= :fromDate
              AND created_at < :toDate
            GROUP BY reward_label
            ORDER BY redemption_count DESC, total_points DESC
            LIMIT :limit
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new DashboardRedemptionRow(
            rs.getString("reward_label"),
            rs.getLong("redemption_count"),
            rs.getBigDecimal("total_points")
        ));
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
}
