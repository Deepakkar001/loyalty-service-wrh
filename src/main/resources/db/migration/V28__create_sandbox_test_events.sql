CREATE TABLE sandbox_test_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    transaction_id VARCHAR(128) NOT NULL,
    request_payload_json JSON NOT NULL,
    response_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_sandbox_tenant_created (tenant_id, created_at),
    INDEX idx_sandbox_tenant_txn (tenant_id, transaction_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS sandbox_test_events;
