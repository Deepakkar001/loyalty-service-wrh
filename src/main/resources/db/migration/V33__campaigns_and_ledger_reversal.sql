-- Campaign module tables + ledger reversal column cleanup (Phase 1).

CREATE TABLE IF NOT EXISTS campaigns (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    programme_uid           VARCHAR(64) NOT NULL,
    campaign_uid            VARCHAR(128) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    campaign_type           VARCHAR(64) NOT NULL,
    occasion_tags           JSON,
    status                  ENUM('DRAFT','ACTIVE','PAUSED','EXHAUSTED','ENDED') NOT NULL DEFAULT 'DRAFT',
    target_segment          JSON,
    eligibility_rules       JSON,
    trigger_event_type      VARCHAR(64),
    offer_config            JSON NOT NULL,
    mutual_excl_group       VARCHAR(64),
    stack_mode              ENUM('ADDITIVE','BEST_OFFER','FIRST_MATCH') NOT NULL DEFAULT 'ADDITIVE',
    budget_total            DECIMAL(18,2) NOT NULL,
    budget_consumed         DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    alert_threshold_pct     DECIMAL(5,2) NOT NULL DEFAULT 80.00,
    priority                INT NOT NULL DEFAULT 0,
    max_participations      INT NULL,
    max_per_customer        INT NULL,
    global_reward_cap       DECIMAL(18,4) NULL,
    merchant_id             VARCHAR(128) NULL,
    valid_from              DATETIME(6) NOT NULL,
    valid_until             DATETIME(6) NOT NULL,
    created_by              VARCHAR(255) NULL,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_campaign (tenant_id, campaign_uid),
    INDEX idx_tenant_prog_status_window (tenant_id, programme_uid, status, valid_from, valid_until),
    INDEX idx_tenant_prog_priority (tenant_id, programme_uid, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campaign_participations (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           VARCHAR(64) NOT NULL,
    programme_uid       VARCHAR(64) NOT NULL,
    campaign_uid        VARCHAR(128) NOT NULL,
    customer_id         VARCHAR(128) NOT NULL,
    event_id            VARCHAR(128) NOT NULL,
    points_awarded      DECIMAL(18,4) NULL,
    cashback_amount     DECIMAL(18,4) NULL,
    participated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_participation (tenant_id, campaign_uid, customer_id, event_id),
    INDEX idx_tenant_customer_campaign (tenant_id, customer_id, campaign_uid),
    INDEX idx_tenant_programme (tenant_id, programme_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campaign_resolution_log (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id             VARCHAR(64) NOT NULL,
    programme_uid         VARCHAR(64) NOT NULL,
    event_id              VARCHAR(128) NOT NULL,
    customer_id           VARCHAR(128) NOT NULL,
    campaigns_evaluated   JSON,
    campaigns_applied     JSON,
    campaigns_dropped     JSON,
    total_points_awarded  DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
    resolution_mode       VARCHAR(64),
    cap_applied           TINYINT(1) NOT NULL DEFAULT 0,
    resolved_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_event_resolution (tenant_id, event_id),
    INDEX idx_tenant_customer (tenant_id, customer_id),
    INDEX idx_tenant_resolved (tenant_id, resolved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Idempotent ledger column/index (skip if you already added reversal_of_ledger_id manually).
SET @rev_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'points_ledger'
      AND COLUMN_NAME = 'reversal_of_ledger_id'
);
SET @rev_col_sql = IF(
    @rev_col_exists = 0,
    'ALTER TABLE points_ledger ADD COLUMN reversal_of_ledger_id BIGINT UNSIGNED NULL',
    'SELECT 1'
);
PREPARE rev_col_stmt FROM @rev_col_sql;
EXECUTE rev_col_stmt;
DEALLOCATE PREPARE rev_col_stmt;

SET @rev_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'points_ledger'
      AND INDEX_NAME = 'idx_reversal_of_ledger'
);
SET @rev_idx_sql = IF(
    @rev_idx_exists = 0,
    'ALTER TABLE points_ledger ADD INDEX idx_reversal_of_ledger (reversal_of_ledger_id)',
    'SELECT 1'
);
PREPARE rev_idx_stmt FROM @rev_idx_sql;
EXECUTE rev_idx_stmt;
DEALLOCATE PREPARE rev_idx_stmt;

UPDATE points_ledger
SET reversal_of_ledger_id = CAST(SUBSTRING(source_campaign_id, 7) AS UNSIGNED),
    source_campaign_id = NULL
WHERE source_campaign_id LIKE 'revof:%';

-- ROLLBACK (manual):
-- ALTER TABLE points_ledger DROP INDEX idx_reversal_of_ledger, DROP COLUMN reversal_of_ledger_id;
-- DROP TABLE IF EXISTS campaign_resolution_log;
-- DROP TABLE IF EXISTS campaign_participations;
-- DROP TABLE IF EXISTS campaigns;
