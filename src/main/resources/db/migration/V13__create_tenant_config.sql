-- ─── TENANT CONFIG (CANONICAL) ─────────────────────────────
-- Master registry + configuration-as-data for each tenant.
-- Canonical reference: LoyaltyOS_Master_Architecture_v3.docx.txt
--
-- This table is the SINGLE source of truth for:
-- - programme identity & points economics
-- - feature flags
-- - event schema (JSON)
-- - webhook configuration (JSON summary; normalized tables exist separately)
--
-- NOTE: This onboarding service previously stored onboarding state in tenant_onboarding.
-- After a tenant reaches ACTIVE, tenant_config becomes the authoritative runtime config.

CREATE TABLE tenant_config (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)     NOT NULL UNIQUE,
    display_name            VARCHAR(255)    NOT NULL,

    subscription_tier       ENUM('STANDARD','PROFESSIONAL','ENTERPRISE')
                                NOT NULL DEFAULT 'STANDARD',
    status                  ENUM('ACTIVE','SUSPENDED','OFFBOARDED')
                                NOT NULL DEFAULT 'ACTIVE',
    ingestion_modes         ENUM('ID_ONLY','FULL_PROFILE','BOTH')
                                NOT NULL DEFAULT 'BOTH',

    -- Points economics / safety controls
    points_currency_rate    DECIMAL(10,6)  NOT NULL DEFAULT 0.010000,
    daily_points_cap        DECIMAL(18,4)  DEFAULT NULL,

    -- Config-as-data fields (extensible)
    feature_flags           JSON           NOT NULL DEFAULT ('{}'),
    event_schema            JSON           DEFAULT NULL,
    webhook_config          JSON           DEFAULT NULL,
    branding                JSON           DEFAULT NULL,

    data_residency_region   ENUM('IN','US','EU','APAC')
                                NOT NULL DEFAULT 'IN',
    max_active_rules        INT            NOT NULL DEFAULT 50,

    created_at              DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)    ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Undo (manual):
-- DROP TABLE tenant_config;

