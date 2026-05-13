CREATE TABLE rule_conditions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    condition_tree JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_rule_conditions_rule (rule_id),
    CONSTRAINT fk_rule_conditions_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_conditions;
