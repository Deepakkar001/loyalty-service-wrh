package com.loyaltyos.analytics.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TierHistoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TierHistoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    public void insertTierHistory(
        String tenantId,
        String programmeUid,
        String customerId,
        String fromTierUid,
        String fromTierName,
        String toTierUid,
        String toTierName,
        BigDecimal balanceAtChange,
        String triggerType
    ) {
        String sql = """
            INSERT INTO tier_history (
              tenant_id, programme_uid, customer_id,
              from_tier_uid, to_tier_uid, from_tier_name, to_tier_name,
              balance_at_change, trigger_type, changed_at
            ) VALUES (
              :tenantId, :programmeUid, :customerId,
              :fromTierUid, :toTierUid, :fromTierName, :toTierName,
              :balanceAtChange, :triggerType, :changedAt
            )
            """;
        var params = new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("programmeUid", programmeUid)
            .addValue("customerId", customerId)
            .addValue("fromTierUid", fromTierUid)
            .addValue("toTierUid", toTierUid)
            .addValue("fromTierName", fromTierName)
            .addValue("toTierName", toTierName)
            .addValue("balanceAtChange", balanceAtChange)
            .addValue("triggerType", triggerType)
            .addValue("changedAt", Instant.now());
        jdbc.update(sql, params);
    }
}
