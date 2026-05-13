-- Core rule definitions (tenant + programme scoped)
CREATE TABLE earn_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    programme_uid VARCHAR(64) NOT NULL DEFAULT 'default',
    rule_uid VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INT NOT NULL DEFAULT 0,
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    trigger_event_type VARCHAR(64) NOT NULL,
    execution_mode ENUM('FIRST_MATCH', 'ALL_MATCHING') NOT NULL DEFAULT 'ALL_MATCHING',
    effective_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL ON UPDATE CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    UNIQUE KEY uk_tenant_programme_rule (tenant_id, programme_uid, rule_uid),
    INDEX idx_tenant_prog_status_trigger (tenant_id, programme_uid, status, trigger_event_type),
    INDEX idx_tenant_prog_priority (tenant_id, programme_uid, priority)
) ENGINE=InnoDB;

CREATE TABLE rule_change_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT UNSIGNED NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    change_type ENUM('CREATED', 'UPDATED', 'STATUS_CHANGED', 'DELETED') NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    before_state JSON NULL,
    after_state JSON NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_rule_change_rule FOREIGN KEY (rule_id) REFERENCES earn_rules(id) ON DELETE CASCADE,
    INDEX idx_tenant_rule (tenant_id, rule_id)
) ENGINE=InnoDB;

-- ROLLBACK:
-- DROP TABLE IF EXISTS rule_change_log;
-- DROP TABLE IF EXISTS earn_rules;
