package com.loyaltyos.analytics.repository;

import com.loyaltyos.analytics.dto.ReversalAdjustmentCustomerRow;
import com.loyaltyos.analytics.dto.ReversalAdjustmentDailyRow;
import com.loyaltyos.analytics.dto.ReversalAdjustmentLedgerRow;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReversalsAdjustmentsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReversalsAdjustmentsQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public long countEntries(
        String tenantId,
        String programmeUid,
        String entryType,
        LocalDate from,
        LocalDate to
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to).addValue("entryType", entryType);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT COUNT(*)
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = :entryType
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            """.formatted(programmeEq);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public long countUniqueCustomers(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT COUNT(DISTINCT pl.customer_id)
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type IN ('REVERSAL', 'ADJUST')
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            """.formatted(programmeEq);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<ReversalAdjustmentLedgerRow> getReversalRows(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to).addValue("limit", limit);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("rev.programme_uid");
        String erProgramme = AnalyticsProgrammeSql.programmeScope("er");
        String sql = """
            SELECT rev.id AS ledger_id,
                   rev.programme_uid,
                   rev.customer_id,
                   rev.points,
                   rev.reversal_of_ledger_id,
                   rev.source_event_id,
                   rev.description,
                   rev.created_by,
                   rev.created_at,
                   orig.entry_type AS original_entry_type,
                   orig.points AS original_points,
                   orig.created_at AS original_created_at,
                   er.name AS rule_name
            FROM points_ledger rev
            LEFT JOIN points_ledger orig
                   ON orig.id = rev.reversal_of_ledger_id
                  AND orig.tenant_id = rev.tenant_id
            LEFT JOIN earn_rules er
                   ON er.id = rev.source_rule_id
                  AND er.tenant_id = rev.tenant_id
                  AND %s
            WHERE rev.tenant_id = :tenantId
              AND %s
              AND rev.entry_type = 'REVERSAL'
              AND rev.created_at >= :fromDate
              AND rev.created_at < :toDate
            ORDER BY rev.created_at DESC
            LIMIT :limit
            """.formatted(erProgramme, programmeEq);
        return jdbc.query(sql, params, (rs, i) -> mapLedgerRow(rs, "REVERSAL", true));
    }

    public List<ReversalAdjustmentLedgerRow> getAdjustmentRows(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to).addValue("limit", limit);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String erProgramme = AnalyticsProgrammeSql.programmeScope("er");
        String sql = """
            SELECT pl.id AS ledger_id,
                   pl.programme_uid,
                   pl.customer_id,
                   pl.points,
                   pl.source_event_id,
                   pl.description,
                   pl.created_by,
                   pl.created_at,
                   er.name AS rule_name
            FROM points_ledger pl
            LEFT JOIN earn_rules er
                   ON er.id = pl.source_rule_id
                  AND er.tenant_id = pl.tenant_id
                  AND %s
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type = 'ADJUST'
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            ORDER BY pl.created_at DESC
            LIMIT :limit
            """.formatted(erProgramme, programmeEq);
        return jdbc.query(sql, params, (rs, i) -> mapLedgerRow(rs, "ADJUST", false));
    }

    public List<ReversalAdjustmentDailyRow> getDailyTrend(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT DATE_FORMAT(pl.created_at, '%%Y-%%m-%%d') AS period,
                   SUM(CASE WHEN pl.entry_type = 'REVERSAL' THEN 1 ELSE 0 END) AS reversal_count,
                   COALESCE(SUM(CASE WHEN pl.entry_type = 'REVERSAL' THEN pl.points ELSE 0 END), 0) AS reversal_points,
                   SUM(CASE WHEN pl.entry_type = 'ADJUST' THEN 1 ELSE 0 END) AS adjustment_count,
                   COALESCE(SUM(CASE WHEN pl.entry_type = 'ADJUST' THEN pl.points ELSE 0 END), 0) AS adjustment_net_points
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type IN ('REVERSAL', 'ADJUST')
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            GROUP BY period
            ORDER BY period ASC
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new ReversalAdjustmentDailyRow(
            rs.getString("period"),
            rs.getLong("reversal_count"),
            rs.getBigDecimal("reversal_points"),
            rs.getLong("adjustment_count"),
            rs.getBigDecimal("adjustment_net_points")
        ));
    }

    public List<ReversalAdjustmentCustomerRow> getTopCustomers(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        var params = rangeParams(tenantId, programmeUid, from, to).addValue("limit", limit);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("pl.programme_uid");
        String sql = """
            SELECT pl.customer_id,
                   SUM(CASE WHEN pl.entry_type = 'REVERSAL' THEN 1 ELSE 0 END) AS reversal_count,
                   COALESCE(SUM(CASE WHEN pl.entry_type = 'REVERSAL' THEN pl.points ELSE 0 END), 0) AS reversal_points,
                   SUM(CASE WHEN pl.entry_type = 'ADJUST' THEN 1 ELSE 0 END) AS adjustment_count,
                   COALESCE(SUM(CASE WHEN pl.entry_type = 'ADJUST' THEN pl.points ELSE 0 END), 0) AS adjustment_net_points
            FROM points_ledger pl
            WHERE pl.tenant_id = :tenantId
              AND %s
              AND pl.entry_type IN ('REVERSAL', 'ADJUST')
              AND pl.created_at >= :fromDate
              AND pl.created_at < :toDate
            GROUP BY pl.customer_id
            ORDER BY (reversal_count + adjustment_count) DESC, reversal_points DESC
            LIMIT :limit
            """.formatted(programmeEq);
        return jdbc.query(sql, params, (rs, i) -> new ReversalAdjustmentCustomerRow(
            rs.getString("customer_id"),
            rs.getLong("reversal_count"),
            rs.getBigDecimal("reversal_points"),
            rs.getLong("adjustment_count"),
            rs.getBigDecimal("adjustment_net_points")
        ));
    }

    private static ReversalAdjustmentLedgerRow mapLedgerRow(
        java.sql.ResultSet rs,
        String entryType,
        boolean includeOriginal
    ) throws java.sql.SQLException {
        ReversalAdjustmentLedgerRow row = new ReversalAdjustmentLedgerRow();
        row.setLedgerId(rs.getLong("ledger_id"));
        row.setEntryType(entryType);
        row.setProgrammeUid(rs.getString("programme_uid"));
        row.setCustomerId(rs.getString("customer_id"));
        BigDecimal points = rs.getBigDecimal("points");
        row.setPoints(points);
        row.setSignedImpact(signedImpact(entryType, points));
        if (includeOriginal) {
            long origId = rs.getLong("reversal_of_ledger_id");
            row.setReversalOfLedgerId(rs.wasNull() ? null : origId);
            row.setOriginalEntryType(rs.getString("original_entry_type"));
            row.setOriginalPoints(rs.getBigDecimal("original_points"));
            row.setOriginalCreatedAt(toInstant(rs.getTimestamp("original_created_at")));
        }
        row.setSourceEventId(rs.getString("source_event_id"));
        row.setRuleName(rs.getString("rule_name"));
        row.setDescription(rs.getString("description"));
        row.setCreatedBy(rs.getString("created_by"));
        row.setCreatedAt(toInstant(rs.getTimestamp("created_at")));
        return row;
    }

    private static BigDecimal signedImpact(String entryType, BigDecimal points) {
        BigDecimal magnitude = points == null ? BigDecimal.ZERO : points;
        if ("REVERSAL".equals(entryType)) {
            return magnitude.negate();
        }
        return magnitude;
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

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
