package com.loyaltyos.analytics.repository;

import com.loyaltyos.analytics.dto.FailedTransactionRow;
import com.loyaltyos.analytics.dto.FailureDailyTrendRow;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FailedAccrualRedemptionQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public FailedAccrualRedemptionQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public long countIssuanceAuditByStatus(
        String tenantId,
        String programmeUid,
        String status,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to).addValue("status", status);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT COUNT(*)
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.status = :status
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                """.formatted(programmeEq);
            Long count = jdbc.queryForObject(sql, params, Long.class);
            return count == null ? 0L : count;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public Double avgFailedIssuanceDurationMs(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT AVG(ria.duration_ms)
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.status = 'FAILED'
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                """.formatted(programmeEq);
            return jdbc.queryForObject(sql, params, Double.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public List<FailedTransactionRow> getFailedIssuanceAuditRows(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to).addValue("limit", limit);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT ria.programme_uid,
                       ria.customer_id,
                       ria.event_id,
                       ria.error_message,
                       ria.duration_ms,
                       ria.processed_at
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.status = 'FAILED'
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                ORDER BY ria.processed_at DESC
                LIMIT :limit
                """.formatted(programmeEq);
            return jdbc.query(sql, params, (rs, i) -> {
                FailedTransactionRow row = new FailedTransactionRow();
                row.setSource("ISSUANCE_AUDIT");
                row.setTransactionType("ACCRUAL");
                row.setProgrammeUid(rs.getString("programme_uid"));
                row.setCustomerId(rs.getString("customer_id"));
                row.setReferenceId(rs.getString("event_id"));
                row.setErrorMessage(rs.getString("error_message"));
                row.setProcessingTimeMs(rs.getObject("duration_ms") != null ? rs.getInt("duration_ms") : null);
                row.setOccurredAt(toInstant(rs.getTimestamp("processed_at")));
                return row;
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<FailedTransactionRow> getFailedEventProcessingRows(
        String tenantId,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        try {
            var params = tenantRangeParams(tenantId, from, to).addValue("limit", limit);
            String sql = """
                SELECT iepl.customer_id,
                       iepl.event_id,
                       iepl.event_type,
                       iepl.processing_status,
                       iepl.error_code,
                       iepl.error_message,
                       iepl.http_status,
                       iepl.processing_time_ms,
                       iepl.created_at
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.processing_status <> 'SUCCESS'
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                ORDER BY iepl.created_at DESC
                LIMIT :limit
                """;
            return jdbc.query(sql, params, (rs, i) -> {
                FailedTransactionRow row = new FailedTransactionRow();
                row.setSource("EVENT_PROCESSING");
                row.setTransactionType("ACCRUAL");
                row.setProgrammeUid(null);
                row.setCustomerId(rs.getString("customer_id"));
                row.setReferenceId(rs.getString("event_id"));
                row.setEventType(rs.getString("event_type"));
                row.setErrorCode(rs.getString("error_code"));
                if (row.getErrorCode() == null || row.getErrorCode().isBlank()) {
                    row.setErrorCode(rs.getString("processing_status"));
                }
                row.setErrorMessage(rs.getString("error_message"));
                row.setHttpStatus(rs.getInt("http_status"));
                row.setProcessingTimeMs(rs.getInt("processing_time_ms"));
                row.setOccurredAt(toInstant(rs.getTimestamp("created_at")));
                return row;
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    public long countFailedEventProcessing(String tenantId, LocalDate from, LocalDate to) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT COUNT(*)
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.processing_status <> 'SUCCESS'
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                """;
            Long count = jdbc.queryForObject(sql, params, Long.class);
            return count == null ? 0L : count;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public long countRedemptionApiAttempts(String tenantId, LocalDate from, LocalDate to, boolean failedOnly) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String statusFilter = failedOnly ? "AND aral.http_status >= 400" : "";
            String sql = """
                SELECT COUNT(*)
                FROM api_request_audit_log aral
                WHERE aral.tenant_id = :tenantId
                  AND aral.request_path LIKE '%%/redemptions%%'
                  AND aral.http_method = 'POST'
                  AND aral.created_at >= :fromDate
                  AND aral.created_at < :toDate
                  %s
                """.formatted(statusFilter);
            Long count = jdbc.queryForObject(sql, params, Long.class);
            return count == null ? 0L : count;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public Double avgFailedRedemptionDurationMs(String tenantId, LocalDate from, LocalDate to) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT AVG(aral.processing_time_ms)
                FROM api_request_audit_log aral
                WHERE aral.tenant_id = :tenantId
                  AND aral.request_path LIKE '%%/redemptions%%'
                  AND aral.http_method = 'POST'
                  AND aral.http_status >= 400
                  AND aral.created_at >= :fromDate
                  AND aral.created_at < :toDate
                """;
            return jdbc.queryForObject(sql, params, Double.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public List<FailedTransactionRow> getFailedRedemptionApiRows(
        String tenantId,
        LocalDate from,
        LocalDate to,
        int limit
    ) {
        try {
            var params = tenantRangeParams(tenantId, from, to).addValue("limit", limit);
            String sql = """
                SELECT aral.customer_id,
                       aral.event_id,
                       aral.request_path,
                       aral.http_status,
                       aral.error_code,
                       aral.error_message,
                       aral.processing_time_ms,
                       aral.created_at
                FROM api_request_audit_log aral
                WHERE aral.tenant_id = :tenantId
                  AND aral.request_path LIKE '%%/redemptions%%'
                  AND aral.http_method = 'POST'
                  AND aral.http_status >= 400
                  AND aral.created_at >= :fromDate
                  AND aral.created_at < :toDate
                ORDER BY aral.created_at DESC
                LIMIT :limit
                """;
            return jdbc.query(sql, params, (rs, i) -> {
                FailedTransactionRow row = new FailedTransactionRow();
                row.setSource("REDEMPTION_API");
                row.setTransactionType("REDEMPTION");
                row.setProgrammeUid(null);
                row.setCustomerId(rs.getString("customer_id"));
                row.setReferenceId(rs.getString("event_id"));
                row.setErrorCode(rs.getString("error_code"));
                row.setErrorMessage(rs.getString("error_message"));
                row.setHttpStatus(rs.getInt("http_status"));
                row.setProcessingTimeMs(rs.getInt("processing_time_ms"));
                row.setOccurredAt(toInstant(rs.getTimestamp("created_at")));
                return row;
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<FailureDailyTrendRow> getDailyFailureTrend(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        Map<String, long[]> byDay = new HashMap<>();
        mergeDailyAccrualFailures(byDay, tenantId, programmeUid, from, to);
        mergeDailyEventFailures(byDay, tenantId, from, to);
        mergeDailyRedemptionFailures(byDay, tenantId, from, to);

        List<FailureDailyTrendRow> rows = new ArrayList<>();
        byDay.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> rows.add(new FailureDailyTrendRow(
                entry.getKey(),
                entry.getValue()[0],
                entry.getValue()[1]
            )));
        return rows;
    }

    private void mergeDailyAccrualFailures(
        Map<String, long[]> byDay,
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT DATE_FORMAT(ria.processed_at, '%%Y-%%m-%%d') AS day_key,
                       COUNT(*) AS failure_count
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.status = 'FAILED'
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                GROUP BY day_key
                """.formatted(programmeEq);
            jdbc.query(sql, params, rs -> {
                String day = rs.getString("day_key");
                long[] bucket = byDay.computeIfAbsent(day, ignored -> new long[2]);
                bucket[0] += rs.getLong("failure_count");
            });
        } catch (Exception ignored) {
            // table may not exist in some environments
        }
    }

    private void mergeDailyEventFailures(
        Map<String, long[]> byDay,
        String tenantId,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT DATE_FORMAT(iepl.created_at, '%%Y-%%m-%%d') AS day_key,
                       COUNT(*) AS failure_count
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.processing_status <> 'SUCCESS'
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                GROUP BY day_key
                """;
            jdbc.query(sql, params, rs -> {
                String day = rs.getString("day_key");
                long[] bucket = byDay.computeIfAbsent(day, ignored -> new long[2]);
                bucket[0] += rs.getLong("failure_count");
            });
        } catch (Exception ignored) {
            // optional source
        }
    }

    private void mergeDailyRedemptionFailures(
        Map<String, long[]> byDay,
        String tenantId,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT DATE_FORMAT(aral.created_at, '%%Y-%%m-%%d') AS day_key,
                       COUNT(*) AS failure_count
                FROM api_request_audit_log aral
                WHERE aral.tenant_id = :tenantId
                  AND aral.request_path LIKE '%%/redemptions%%'
                  AND aral.http_method = 'POST'
                  AND aral.http_status >= 400
                  AND aral.created_at >= :fromDate
                  AND aral.created_at < :toDate
                GROUP BY day_key
                """;
            jdbc.query(sql, params, rs -> {
                String day = rs.getString("day_key");
                long[] bucket = byDay.computeIfAbsent(day, ignored -> new long[2]);
                bucket[1] += rs.getLong("failure_count");
            });
        } catch (Exception ignored) {
            // optional source
        }
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

    private static MapSqlParameterSource tenantRangeParams(String tenantId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("fromDate", from.atStartOfDay())
            .addValue("toDate", to.plusDays(1).atStartOfDay());
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
