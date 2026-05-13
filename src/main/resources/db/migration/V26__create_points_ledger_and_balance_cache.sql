-- Ledger is owned by future RewardExecutionService; rule engine does not insert here in v1.
CREATE TABLE points_ledger (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    idempotency_key VARCHAR(128) NOT NULL,
    entry_type ENUM('CREDIT','DEBIT','EXPIRE','REVERSAL','ADJUST') NOT NULL,
    points DECIMAL(18, 4) NOT NULL,
    source_rule_id BIGINT UNSIGNED NULL,
    source_event_id VARCHAR(128) NULL,
    source_campaign_id VARCHAR(128) NULL,
    expires_at DATETIME(6) NULL,
    description VARCHAR(512) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_customer_idempotency (tenant_id, customer_id, idempotency_key),
    INDEX idx_tenant_customer (tenant_id, customer_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB;

CREATE TABLE customer_balance_cache (
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    customer_id VARCHAR(128) NOT NULL,
    balance DECIMAL(18, 4) NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (tenant_id, programme_uid, customer_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS customer_balance_cache;
-- DROP TABLE IF EXISTS points_ledger;
