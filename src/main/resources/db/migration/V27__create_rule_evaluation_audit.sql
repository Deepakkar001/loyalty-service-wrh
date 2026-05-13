CREATE TABLE rule_evaluation_audit (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    customer_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    success TINYINT(1) NOT NULL,
    trace_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_tenant_created (tenant_id, created_at),
    INDEX idx_tenant_event (tenant_id, event_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_evaluation_audit;
