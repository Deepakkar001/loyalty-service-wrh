package com.loyaltyos.campaigns.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignExpirationService {

    private static final Logger log = LoggerFactory.getLogger(CampaignExpirationService.class);

    /**
     * Marks campaigns whose schedule has ended ({@code valid_until} in the past).
     * Applies to DRAFT, ACTIVE, and PAUSED — terminal statuses (EXHAUSTED, ENDED, EXPIRED) are unchanged.
     */
    private static final String SWEEP_EXPIRED_SQL = """
        UPDATE campaigns
        SET status = 'EXPIRED'
        WHERE status IN ('DRAFT', 'ACTIVE', 'PAUSED')
          AND valid_until < UTC_TIMESTAMP(3)
        """;

    private final JdbcTemplate jdbcTemplate;

    public CampaignExpirationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Transactional
    public int sweepCampaignsPastValidUntil() {
        int updated = jdbcTemplate.update(SWEEP_EXPIRED_SQL);
        if (updated > 0) {
            log.info("Campaign expiration sweep marked {} campaign(s) EXPIRED", updated);
        }
        return updated;
    }
}
