package com.loyaltyos.campaigns.repository;

import com.loyaltyos.campaigns.dto.CampaignParticipationAggregate;
import com.loyaltyos.campaigns.dto.CampaignParticipationTrendRow;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CampaignAnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CampaignAnalyticsQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public List<CampaignParticipationTrendRow> getDailyParticipationTrend(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT DATE_FORMAT(daily.day_bucket, '%Y-%m-%d') AS period,
                   daily.participations,
                   daily.points_issued,
                   daily.cashback_recorded
            FROM (
              SELECT DATE(cp.participated_at) AS day_bucket,
                     COUNT(*) AS participations,
                     COALESCE(SUM(cp.points_awarded), 0) AS points_issued,
                     COALESCE(SUM(cp.cashback_amount), 0) AS cashback_recorded
              FROM campaign_participations cp
              WHERE cp.tenant_id = :tenantId
                AND cp.programme_uid = :programmeUid
                AND cp.participated_at >= :fromDate
                AND cp.participated_at < :toDate
              GROUP BY DATE(cp.participated_at)
            ) daily
            ORDER BY daily.day_bucket ASC
            """;
        return jdbc.query(
            sql,
            rangeParams(tenantId, programmeUid, from, to),
            (rs, i) -> new CampaignParticipationTrendRow(
                rs.getString("period"),
                rs.getLong("participations"),
                rs.getBigDecimal("points_issued"),
                rs.getBigDecimal("cashback_recorded")
            )
        );
    }

    public Map<String, CampaignParticipationAggregate> getParticipationAggregatesByCampaign(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT cp.campaign_uid,
                   COUNT(*) AS participations,
                   COUNT(DISTINCT cp.customer_id) AS unique_customers,
                   COALESCE(SUM(cp.points_awarded), 0) AS points_issued,
                   COALESCE(SUM(cp.cashback_amount), 0) AS cashback_recorded,
                   MIN(cp.participated_at) AS first_participation_at,
                   MAX(cp.participated_at) AS last_participation_at
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
              AND cp.participated_at >= :fromDate
              AND cp.participated_at < :toDate
            GROUP BY cp.campaign_uid
            """;
        List<CampaignParticipationAggregate> rows = jdbc.query(
            sql,
            rangeParams(tenantId, programmeUid, from, to),
            (rs, i) -> new CampaignParticipationAggregate(
                rs.getString("campaign_uid"),
                rs.getLong("participations"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("points_issued"),
                rs.getBigDecimal("cashback_recorded"),
                toInstant(rs.getTimestamp("first_participation_at")),
                toInstant(rs.getTimestamp("last_participation_at"))
            )
        );
        Map<String, CampaignParticipationAggregate> map = new HashMap<>();
        for (CampaignParticipationAggregate row : rows) {
            map.put(row.campaignUid(), row);
        }
        return map;
    }

    public Map<String, CampaignParticipationAggregate> getAllTimeParticipationAggregatesByCampaign(
        String tenantId,
        String programmeUid
    ) {
        String sql = """
            SELECT cp.campaign_uid,
                   COUNT(*) AS participations,
                   COUNT(DISTINCT cp.customer_id) AS unique_customers,
                   COALESCE(SUM(cp.points_awarded), 0) AS points_issued,
                   COALESCE(SUM(cp.cashback_amount), 0) AS cashback_recorded,
                   MIN(cp.participated_at) AS first_participation_at,
                   MAX(cp.participated_at) AS last_participation_at
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
            GROUP BY cp.campaign_uid
            """;
        List<CampaignParticipationAggregate> rows = jdbc.query(
            sql,
            new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("programmeUid", programmeUid),
            (rs, i) -> new CampaignParticipationAggregate(
                rs.getString("campaign_uid"),
                rs.getLong("participations"),
                rs.getLong("unique_customers"),
                rs.getBigDecimal("points_issued"),
                rs.getBigDecimal("cashback_recorded"),
                toInstant(rs.getTimestamp("first_participation_at")),
                toInstant(rs.getTimestamp("last_participation_at"))
            )
        );
        Map<String, CampaignParticipationAggregate> map = new HashMap<>();
        for (CampaignParticipationAggregate row : rows) {
            map.put(row.campaignUid(), row);
        }
        return map;
    }

    public long countProgrammeParticipations(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COUNT(*)
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
              AND cp.participated_at >= :fromDate
              AND cp.participated_at < :toDate
            """;
        Long count = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), Long.class);
        return count == null ? 0L : count;
    }

    public long countProgrammeUniqueCustomers(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COUNT(DISTINCT cp.customer_id)
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
              AND cp.participated_at >= :fromDate
              AND cp.participated_at < :toDate
            """;
        Long count = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), Long.class);
        return count == null ? 0L : count;
    }

    public BigDecimal sumProgrammePoints(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COALESCE(SUM(cp.points_awarded), 0)
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
              AND cp.participated_at >= :fromDate
              AND cp.participated_at < :toDate
            """;
        BigDecimal sum = jdbc.queryForObject(sql, rangeParams(tenantId, programmeUid, from, to), BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public BigDecimal sumProgrammeCashback(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT COALESCE(SUM(cp.cashback_amount), 0)
            FROM campaign_participations cp
            WHERE cp.tenant_id = :tenantId
              AND cp.programme_uid = :programmeUid
              AND cp.participated_at >= :fromDate
              AND cp.participated_at < :toDate
            """;
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

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
