package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.model.BudgetDecrementResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignBudgetService {

    private static final Logger log = LoggerFactory.getLogger(CampaignBudgetService.class);

    private static final String DECREMENT_SQL = """
        UPDATE campaigns
        SET budget_consumed = budget_consumed + ?
        WHERE tenant_id = ?
          AND campaign_uid = ?
          AND status = 'ACTIVE'
          AND budget_consumed + ? <= budget_total
        """;

    private static final String SELECT_BUDGET_SQL = """
        SELECT programme_uid, budget_consumed, budget_total, alert_threshold_pct, status
        FROM campaigns
        WHERE tenant_id = ? AND campaign_uid = ?
        """;

    private static final String MARK_EXHAUSTED_SQL = """
        UPDATE campaigns
        SET status = 'EXHAUSTED'
        WHERE tenant_id = ?
          AND campaign_uid = ?
          AND budget_consumed >= budget_total
          AND status = 'ACTIVE'
        """;

    private final JdbcTemplate jdbcTemplate;
    private final CampaignBudgetAlertNotifier alertNotifier;

    public CampaignBudgetService(JdbcTemplate jdbcTemplate, CampaignBudgetAlertNotifier alertNotifier) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.alertNotifier = Objects.requireNonNull(alertNotifier, "alertNotifier");
    }

    /**
     * Atomic budget decrement — single UPDATE only. Returns {@code success=false} when no row updated
     * (inactive, exhausted, or insufficient budget).
     */
    @Transactional
    public BudgetDecrementResult tryDecrementBudget(String tenantId, String campaignUid, BigDecimal cost) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(campaignUid, "campaignUid");
        if (cost == null || cost.signum() <= 0) {
            return new BudgetDecrementResult(true, false, false, null, null);
        }

        BigDecimal normalized = cost.setScale(2, RoundingMode.HALF_UP);
        int rows = jdbcTemplate.update(
            DECREMENT_SQL,
            normalized,
            tenantId,
            campaignUid,
            normalized
        );

        if (rows == 0) {
            return BudgetDecrementResult.failed();
        }

        return jdbcTemplate.query(
            SELECT_BUDGET_SQL,
            rs -> {
                if (!rs.next()) {
                    return BudgetDecrementResult.failed();
                }
                String programmeUid = rs.getString("programme_uid");
                BigDecimal consumed = rs.getBigDecimal("budget_consumed");
                BigDecimal total = rs.getBigDecimal("budget_total");
                BigDecimal alertPct = rs.getBigDecimal("alert_threshold_pct");
                boolean alertCrossed = isAlertThresholdCrossed(consumed, total, alertPct);
                if (alertCrossed) {
                    log.warn(
                        "Campaign budget alert: tenant={} campaignUid={} consumed={} total={} thresholdPct={}",
                        tenantId,
                        campaignUid,
                        consumed,
                        total,
                        alertPct
                    );
                    alertNotifier.notifyBudgetAlert(
                        tenantId,
                        programmeUid,
                        campaignUid,
                        consumed,
                        total,
                        alertPct
                    );
                }

                if (consumed.compareTo(total) >= 0) {
                    jdbcTemplate.update(MARK_EXHAUSTED_SQL, tenantId, campaignUid);
                    alertNotifier.notifyBudgetExhausted(tenantId, programmeUid, campaignUid);
                }

                return new BudgetDecrementResult(true, false, alertCrossed, consumed, total);
            },
            tenantId,
            campaignUid
        );
    }

    private static boolean isAlertThresholdCrossed(BigDecimal consumed, BigDecimal total, BigDecimal alertPct) {
        if (total == null || total.signum() <= 0 || alertPct == null) {
            return false;
        }
        BigDecimal ratio = consumed.multiply(new BigDecimal("100"))
            .divide(total, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(alertPct) >= 0;
    }
}
