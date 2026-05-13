CREATE TABLE rule_actions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    action_uid VARCHAR(128) NOT NULL,
    action_type ENUM(
        'AWARD_POINTS',
        'GRANT_BADGE',
        'ISSUE_VOUCHER',
        'TRIGGER_NOTIFICATION',
        'WEBHOOK_CALLBACK',
        'UPGRADE_TIER',
        'APPLY_MULTIPLIER'
    ) NOT NULL,
    formula VARCHAR(512) NULL,
    config JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_tenant_action (tenant_id, action_uid),
    CONSTRAINT fk_rule_actions_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_actions;
