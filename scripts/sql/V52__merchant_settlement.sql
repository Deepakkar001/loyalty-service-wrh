ALTER TABLE merchants
    ADD COLUMN settlement_hold TINYINT(1) NOT NULL DEFAULT 0 AFTER integration_test_passed_at;

CREATE TABLE merchant_settlement_cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cycle_uid VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    merchant_uid VARCHAR(128) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_points BIGINT NOT NULL DEFAULT 0,
    total_monetary_value DECIMAL(18, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finalized_at TIMESTAMP(6) NULL,
    UNIQUE KEY uk_settlement_cycle_uid (cycle_uid),
    INDEX idx_settlement_merchant (tenant_id, merchant_uid),
    INDEX idx_settlement_status (tenant_id, status)
);

CREATE TABLE settlement_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_item_uid VARCHAR(128) NOT NULL,
    cycle_id BIGINT NOT NULL,
    txn_reference VARCHAR(256) NOT NULL,
    points_amount BIGINT NOT NULL DEFAULT 0,
    monetary_value DECIMAL(18, 2) NOT NULL DEFAULT 0,
    disputed TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_settlement_line_item_uid (line_item_uid),
    INDEX idx_settlement_line_cycle (cycle_id),
    CONSTRAINT fk_settlement_line_cycle FOREIGN KEY (cycle_id) REFERENCES merchant_settlement_cycles (id)
);

CREATE TABLE settlement_disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispute_uid VARCHAR(128) NOT NULL,
    line_item_id BIGINT NOT NULL,
    reason VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    hold_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    resolution VARCHAR(1024) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at TIMESTAMP(6) NULL,
    UNIQUE KEY uk_settlement_dispute_uid (dispute_uid),
    INDEX idx_settlement_dispute_line (line_item_id),
    CONSTRAINT fk_settlement_dispute_line FOREIGN KEY (line_item_id) REFERENCES settlement_line_items (id)
);
