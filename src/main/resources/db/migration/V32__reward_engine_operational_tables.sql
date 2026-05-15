-- Issuance audit (append-only operational log; not the financial ledger).
CREATE TABLE reward_issuance_audit (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    programme_uid   VARCHAR(64) NOT NULL DEFAULT 'default',
    customer_id     VARCHAR(128) NOT NULL,
    event_id        VARCHAR(128) NOT NULL,
    total_points_awarded DECIMAL(18, 4) NOT NULL,
    rule_count      INT NOT NULL,
    status          VARCHAR(32) NOT NULL,
    error_message   TEXT,
    ledger_ids      JSON,
    duration_ms     INT,
    processed_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_tenant_customer_event (tenant_id, customer_id, event_id),
    INDEX idx_tenant_programme (tenant_id, programme_uid),
    INDEX idx_status (status),
    INDEX idx_processed (processed_at)
) ENGINE=InnoDB;

-- Nightly / on-demand expiry batch metadata.
CREATE TABLE points_expiry_jobs (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    job_uid          VARCHAR(128) NOT NULL,
    batch_date       DATE NOT NULL,
    executed_at      DATETIME(6),
    total_expired    BIGINT,
    customers_affected BIGINT,
    error_message    TEXT,
    status           VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_job_uid (job_uid),
    INDEX idx_tenant_date (tenant_id, batch_date),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Reconciliation: ledger-derived balance vs cache (manual or scheduled).
CREATE TABLE balance_reconciliation_logs (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    programme_uid           VARCHAR(64) NOT NULL DEFAULT 'default',
    customer_id             VARCHAR(128),
    expected_balance        DECIMAL(18, 4),
    cached_balance          DECIMAL(18, 4),
    variance                DECIMAL(18, 4),
    reconciliation_action VARCHAR(32) NOT NULL,
    executed_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_tenant_programme (tenant_id, programme_uid),
    INDEX idx_tenant_customer (tenant_id, customer_id)
) ENGINE=InnoDB;
