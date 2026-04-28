-- ============================================================
-- LoyaltyOS v3.0 — Tenant Onboarding Schema
-- MySQL 8.0 | InnoDB | UTF8MB4
-- This file is managed by Flyway — never edit manually
-- ============================================================

-- ─── TENANT ONBOARDING ─────────────────────────────────────
-- Stores the 6-stage onboarding workflow state for each tenant.
-- The tenant_config table (master registry) is owned by the
-- tenant-config-service. This table owns only onboarding state.
-- After a tenant reaches ACTIVE, tenant_config is the authority.

CREATE TABLE tenant_onboarding (
    id                          BIGINT UNSIGNED         AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   VARCHAR(64)             NOT NULL,
    company_name                VARCHAR(255)            NOT NULL,
    slug                        VARCHAR(100)            NOT NULL,
    email                       VARCHAR(255)            NOT NULL,
    password_hash               VARCHAR(255)            NOT NULL,
    business_category           ENUM(
                                    'RETAIL','ECOMMERCE','FINTECH','HOSPITALITY',
                                    'GAMING','HEALTHCARE','TELECOM','OTHER'
                                ) NOT NULL,
    onboarding_status           ENUM(
                                    'PENDING_EMAIL_VERIFICATION','EMAIL_VERIFIED',
                                    'AGREEMENT_PENDING','AGREEMENT_SIGNED',
                                    'CONFIGURED','SANDBOX_TESTING',
                                    'ACTIVE','SUSPENDED','TERMINATED'
                                ) NOT NULL DEFAULT 'PENDING_EMAIL_VERIFICATION',
    identity_mode               ENUM('ID_ONLY','FULL_PROFILE','BOTH')
                                    NOT NULL DEFAULT 'FULL_PROFILE',
    subscription_tier           ENUM('STANDARD','PROFESSIONAL','ENTERPRISE')
                                    NOT NULL DEFAULT 'STANDARD',
    data_residency_region       ENUM('IN','US','EU','APAC')
                                    NOT NULL DEFAULT 'IN',
    website_url                 VARCHAR(500),
    country_code                CHAR(2)                 NOT NULL,
    timezone                    VARCHAR(100)            NOT NULL DEFAULT 'UTC',
    -- Email verification
    email_verified              TINYINT(1)              NOT NULL DEFAULT 0,
    email_verification_token    VARCHAR(255),
    email_verification_expiry   DATETIME(6),
    -- Identity mode lock (set to 1 after first event ingested — see architecture doc)
    is_identity_mode_locked     TINYINT(1)              NOT NULL DEFAULT 0,
    -- Temporal tracking
    created_at                  DATETIME(6)             NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)             ON UPDATE CURRENT_TIMESTAMP(6),
    activated_at                DATETIME(6),
    suspended_at                DATETIME(6),
    terminated_at               DATETIME(6),
    -- Audit
    created_by_admin_id         VARCHAR(128),
    -- Optimistic locking
    version                     INT UNSIGNED            NOT NULL DEFAULT 0,
    UNIQUE KEY uq_tenant_id   (tenant_id),
    UNIQUE KEY uq_email       (email),
    UNIQUE KEY uq_slug        (slug),
    INDEX idx_status          (onboarding_status),
    INDEX idx_email_verified  (email_verified, onboarding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Prevent identity_mode change once locked
DELIMITER //
CREATE TRIGGER trg_prevent_identity_mode_change
BEFORE UPDATE ON tenant_onboarding
FOR EACH ROW
BEGIN
    IF OLD.is_identity_mode_locked = 1
       AND NEW.identity_mode != OLD.identity_mode THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'identity_mode is locked after first event ingestion and cannot be changed';
    END IF;
END//
DELIMITER ;

