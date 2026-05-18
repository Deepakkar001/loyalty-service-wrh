package com.loyaltyos.campaigns.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignExhaustionService {

    private static final Logger log = LoggerFactory.getLogger(CampaignExhaustionService.class);

    private static final String SWEEP_EXHAUSTED_SQL = """
        UPDATE campaigns
        SET status = 'EXHAUSTED'
        WHERE status = 'ACTIVE'
          AND budget_consumed >= budget_total
        """;

    private final JdbcTemplate jdbcTemplate;

    public CampaignExhaustionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Transactional
    public int sweepActiveCampaignsOverBudget() {
        int updated = jdbcTemplate.update(SWEEP_EXHAUSTED_SQL);
        if (updated > 0) {
            log.info("Campaign exhaustion sweep marked {} campaign(s) EXHAUSTED", updated);
        }
        return updated;
    }
}
