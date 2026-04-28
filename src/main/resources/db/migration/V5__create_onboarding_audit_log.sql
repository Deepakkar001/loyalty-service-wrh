-- ─── ONBOARDING AUDIT LOG ──────────────────────────────────
-- IMMUTABLE: Every state change during onboarding is recorded here.
-- TRIGGERS prevent any UPDATE or DELETE — this is a compliance record.
-- Mirrors the platform-wide audit_log table but scoped to onboarding events.
-- Note: The platform-wide audit_log table is owned by a separate service.
-- This table is owned exclusively by tenant-onboarding-service.

CREATE TABLE onboarding_audit_log (
    id              BIGINT UNSIGNED     AUTO_INCREMENT PRIMARY KEY,
    tenant_id       VARCHAR(64)         NOT NULL,
    action          VARCHAR(128)        NOT NULL,
    actor_id        VARCHAR(128),
    actor_role      VARCHAR(64),
    before_state    JSON,
    after_state     JSON,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_tenant_id     (tenant_id),
    INDEX idx_created_at    (tenant_id, created_at),
    INDEX idx_action        (tenant_id, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- IMMUTABILITY TRIGGERS
DELIMITER //

CREATE TRIGGER trg_audit_no_update
BEFORE UPDATE ON onboarding_audit_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'onboarding_audit_log is immutable — UPDATE not allowed';
END//

CREATE TRIGGER trg_audit_no_delete
BEFORE DELETE ON onboarding_audit_log
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'onboarding_audit_log is immutable — DELETE not allowed';
END//

DELIMITER ;

