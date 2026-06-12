package com.loyaltyos.analytics.repository;

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
public class SlaPerformanceQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SlaPerformanceQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public OperationAggregate getApiAggregate(String tenantId, LocalDate from, LocalDate to, String pathPattern) {
        var params = tenantRangeParams(tenantId, from, to).addValue("pathPattern", pathPattern);
        String sql = """
            SELECT COUNT(*) AS total_ops,
                   COALESCE(SUM(CASE WHEN aral.http_status >= 200 AND aral.http_status < 300 THEN 1 ELSE 0 END), 0) AS success_ops,
                   COALESCE(AVG(aral.processing_time_ms), 0) AS avg_ms,
                   COALESCE(MAX(aral.processing_time_ms), 0) AS max_ms
            FROM api_request_audit_log aral
            WHERE aral.tenant_id = :tenantId
              AND aral.created_at >= :fromDate
              AND aral.created_at < :toDate
              AND aral.request_path LIKE :pathPattern
            """;
        return jdbc.queryForObject(sql, params, (rs, i) -> new OperationAggregate(
            rs.getLong("total_ops"),
            rs.getLong("success_ops"),
            rs.getInt("avg_ms"),
            rs.getInt("max_ms")
        ));
    }

    public List<Integer> getApiLatencies(String tenantId, LocalDate from, LocalDate to, String pathPattern) {
        var params = tenantRangeParams(tenantId, from, to).addValue("pathPattern", pathPattern);
        String sql = """
            SELECT aral.processing_time_ms
            FROM api_request_audit_log aral
            WHERE aral.tenant_id = :tenantId
              AND aral.created_at >= :fromDate
              AND aral.created_at < :toDate
              AND aral.request_path LIKE :pathPattern
              AND aral.http_status >= 200
              AND aral.http_status < 300
            ORDER BY aral.processing_time_ms ASC
            """;
        return jdbc.query(sql, params, (rs, i) -> rs.getInt("processing_time_ms"));
    }

    public Map<String, OperationAggregate> getApiAggregatesByOperation(String tenantId, LocalDate from, LocalDate to) {
        var params = tenantRangeParams(tenantId, from, to);
        String sql = """
            SELECT op_key,
                   COUNT(*) AS total_ops,
                   COALESCE(SUM(CASE WHEN aral.http_status >= 200 AND aral.http_status < 300 THEN 1 ELSE 0 END), 0) AS success_ops,
                   COALESCE(AVG(aral.processing_time_ms), 0) AS avg_ms,
                   COALESCE(MAX(aral.processing_time_ms), 0) AS max_ms
            FROM (
              SELECT aral.*,
                     CASE
                       WHEN aral.request_path LIKE '%%/events/process%%' THEN 'EVENT_PROCESS'
                       WHEN aral.request_path LIKE '%%/redemptions/validate%%' THEN 'REDEMPTION_VALIDATE'
                       WHEN aral.request_path LIKE '%%/redemptions%%' THEN 'REDEMPTION'
                       WHEN aral.request_path LIKE '%%/balance%%' THEN 'BALANCE'
                       WHEN aral.request_path LIKE '%%/events/validate%%' THEN 'EVENT_VALIDATE'
                       ELSE 'OTHER'
                     END AS op_key
              FROM api_request_audit_log aral
              WHERE aral.tenant_id = :tenantId
                AND aral.created_at >= :fromDate
                AND aral.created_at < :toDate
            ) aral
            GROUP BY op_key
            """;
        Map<String, OperationAggregate> result = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            result.put(rs.getString("op_key"), new OperationAggregate(
                rs.getLong("total_ops"),
                rs.getLong("success_ops"),
                rs.getInt("avg_ms"),
                rs.getInt("max_ms")
            ));
        });
        return result;
    }

    public OperationAggregate getIssuanceAggregate(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT COUNT(*) AS total_ops,
                       COALESCE(SUM(CASE WHEN ria.status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_ops,
                       COALESCE(AVG(CASE WHEN ria.status = 'SUCCESS' THEN ria.duration_ms END), 0) AS avg_ms,
                       COALESCE(MAX(CASE WHEN ria.status = 'SUCCESS' THEN ria.duration_ms END), 0) AS max_ms
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                """.formatted(programmeEq);
            OperationAggregate agg = jdbc.queryForObject(sql, params, (rs, i) -> new OperationAggregate(
                rs.getLong("total_ops"),
                rs.getLong("success_ops"),
                rs.getInt("avg_ms"),
                rs.getInt("max_ms")
            ));
            return agg == null ? OperationAggregate.empty() : agg;
        } catch (Exception ex) {
            return OperationAggregate.empty();
        }
    }

    public List<Integer> getIssuanceLatencies(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to,
        String status
    ) {
        try {
            var params = rangeParams(tenantId, programmeUid, from, to).addValue("status", status);
            String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
            String sql = """
                SELECT ria.duration_ms
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.status = :status
                  AND ria.duration_ms IS NOT NULL
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                ORDER BY ria.duration_ms ASC
                """.formatted(programmeEq);
            return jdbc.query(sql, params, (rs, i) -> rs.getInt("duration_ms"));
        } catch (Exception ex) {
            return List.of();
        }
    }

    public OperationAggregate getEventProcessingAggregate(String tenantId, LocalDate from, LocalDate to, boolean successOnly) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String statusFilter = successOnly
                ? "AND iepl.processing_status = 'SUCCESS'"
                : "";
            String sql = """
                SELECT COUNT(*) AS total_ops,
                       COALESCE(SUM(CASE WHEN iepl.processing_status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_ops,
                       COALESCE(AVG(iepl.processing_time_ms), 0) AS avg_ms,
                       COALESCE(MAX(iepl.processing_time_ms), 0) AS max_ms
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                  %s
                """.formatted(statusFilter);
            return jdbc.queryForObject(sql, params, (rs, i) -> new OperationAggregate(
                rs.getLong("total_ops"),
                rs.getLong("success_ops"),
                rs.getInt("avg_ms"),
                rs.getInt("max_ms")
            ));
        } catch (Exception ex) {
            return OperationAggregate.empty();
        }
    }

    public List<Integer> getEventProcessingLatencies(String tenantId, LocalDate from, LocalDate to) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT iepl.processing_time_ms
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.processing_status = 'SUCCESS'
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                ORDER BY iepl.processing_time_ms ASC
                """;
            return jdbc.query(sql, params, (rs, i) -> rs.getInt("processing_time_ms"));
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<DailyAggregate> getDailyTrend(String tenantId, String programmeUid, LocalDate from, LocalDate to) {
        var params = rangeParams(tenantId, programmeUid, from, to);
        String programmeEq = AnalyticsProgrammeSql.programmeColumnEqualsParam("ria.programme_uid");
        Map<String, DailyAggregate.Builder> byDay = new HashMap<>();

        mergeApiDaily(byDay, tenantId, from, to);
        mergeIssuanceDaily(byDay, params, programmeEq);
        mergeEventDaily(byDay, tenantId, from, to);

        List<DailyAggregate> rows = new ArrayList<>(byDay.values().stream().map(DailyAggregate.Builder::build).toList());
        rows.sort((a, b) -> a.period().compareTo(b.period()));
        return rows;
    }

    private void mergeApiDaily(Map<String, DailyAggregate.Builder> byDay, String tenantId, LocalDate from, LocalDate to) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT DATE_FORMAT(aral.created_at, '%%Y-%%m-%%d') AS period,
                       COUNT(*) AS total_ops,
                       COALESCE(SUM(CASE WHEN aral.http_status >= 200 AND aral.http_status < 300 THEN 1 ELSE 0 END), 0) AS success_ops,
                       COALESCE(AVG(aral.processing_time_ms), 0) AS avg_ms
                FROM api_request_audit_log aral
                WHERE aral.tenant_id = :tenantId
                  AND aral.created_at >= :fromDate
                  AND aral.created_at < :toDate
                GROUP BY period
                """;
            jdbc.query(sql, params, rs -> {
                String period = rs.getString("period");
                DailyAggregate.Builder builder = byDay.computeIfAbsent(period, DailyAggregate.Builder::new);
                builder.apiRequests = rs.getLong("total_ops");
                builder.apiSuccess = rs.getLong("success_ops");
                builder.apiAvgMs = rs.getInt("avg_ms");
            });
        } catch (Exception ignored) {
            // optional
        }
    }

    private void mergeIssuanceDaily(
        Map<String, DailyAggregate.Builder> byDay,
        MapSqlParameterSource params,
        String programmeEq
    ) {
        try {
            String sql = """
                SELECT DATE_FORMAT(ria.processed_at, '%%Y-%%m-%%d') AS period,
                       COUNT(*) AS total_ops,
                       COALESCE(SUM(CASE WHEN ria.status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_ops,
                       COALESCE(AVG(CASE WHEN ria.status = 'SUCCESS' THEN ria.duration_ms END), 0) AS avg_ms
                FROM reward_issuance_audit ria
                WHERE ria.tenant_id = :tenantId
                  AND %s
                  AND ria.processed_at >= :fromDate
                  AND ria.processed_at < :toDate
                GROUP BY period
                """.formatted(programmeEq);
            jdbc.query(sql, params, rs -> {
                String period = rs.getString("period");
                DailyAggregate.Builder builder = byDay.computeIfAbsent(period, DailyAggregate.Builder::new);
                builder.issuanceAttempts = rs.getLong("total_ops");
                builder.issuanceSuccess = rs.getLong("success_ops");
                builder.issuanceAvgMs = rs.getInt("avg_ms");
            });
        } catch (Exception ignored) {
            // optional
        }
    }

    private void mergeEventDaily(
        Map<String, DailyAggregate.Builder> byDay,
        String tenantId,
        LocalDate from,
        LocalDate to
    ) {
        try {
            var params = tenantRangeParams(tenantId, from, to);
            String sql = """
                SELECT DATE_FORMAT(iepl.created_at, '%%Y-%%m-%%d') AS period,
                       COUNT(*) AS total_ops,
                       COALESCE(SUM(CASE WHEN iepl.processing_status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS success_ops,
                       COALESCE(AVG(iepl.processing_time_ms), 0) AS avg_ms
                FROM integration_event_processing_log iepl
                WHERE iepl.tenant_id = :tenantId
                  AND iepl.created_at >= :fromDate
                  AND iepl.created_at < :toDate
                GROUP BY period
                """;
            jdbc.query(sql, params, rs -> {
                String period = rs.getString("period");
                DailyAggregate.Builder builder = byDay.computeIfAbsent(period, DailyAggregate.Builder::new);
                builder.eventAttempts = rs.getLong("total_ops");
                builder.eventSuccess = rs.getLong("success_ops");
                builder.eventAvgMs = rs.getInt("avg_ms");
            });
        } catch (Exception ignored) {
            // optional
        }
    }

    public record OperationAggregate(long totalOps, long successOps, int avgMs, int maxMs) {
        public static OperationAggregate empty() {
            return new OperationAggregate(0L, 0L, 0, 0);
        }
    }

    public record DailyAggregate(
        String period,
        long apiRequests,
        long apiSuccess,
        int apiAvgMs,
        long issuanceAttempts,
        long issuanceSuccess,
        int issuanceAvgMs,
        long eventAttempts,
        long eventSuccess,
        int eventAvgMs
    ) {
        static final class Builder {
            String period;
            long apiRequests;
            long apiSuccess;
            int apiAvgMs;
            long issuanceAttempts;
            long issuanceSuccess;
            int issuanceAvgMs;
            long eventAttempts;
            long eventSuccess;
            int eventAvgMs;

            Builder(String period) {
                this.period = period;
            }

            DailyAggregate build() {
                return new DailyAggregate(
                    period,
                    apiRequests,
                    apiSuccess,
                    apiAvgMs,
                    issuanceAttempts,
                    issuanceSuccess,
                    issuanceAvgMs,
                    eventAttempts,
                    eventSuccess,
                    eventAvgMs
                );
            }
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
}
