ALTER TABLE merchant_credentials
    ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active,
    ADD COLUMN invite_token_hash VARCHAR(128) NULL AFTER must_change_password,
    ADD COLUMN invite_token_expires_at TIMESTAMP(6) NULL AFTER invite_token_hash,
    ADD COLUMN invite_accepted_at TIMESTAMP(6) NULL AFTER invite_token_expires_at;

CREATE TABLE merchant_budget_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    merchant_uid VARCHAR(128) NOT NULL,
    campaign_uid VARCHAR(128) NOT NULL,
    alert_threshold_pct DECIMAL(5,2) NOT NULL,
    budget_consumed DECIMAL(18,2) NOT NULL,
    budget_total DECIMAL(18,2) NOT NULL,
    notified_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_merchant_campaign_alert (tenant_id, campaign_uid, alert_threshold_pct),
    INDEX idx_merchant (tenant_id, merchant_uid)
);
