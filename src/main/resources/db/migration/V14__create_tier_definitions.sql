-- ─── TIER DEFINITIONS (CANONICAL) ──────────────────────────
-- Canonical reference: LoyaltyOS_Master_Architecture_v3.docx.txt
--
-- Tenants define up to 10 tiers with thresholds and multipliers.

CREATE TABLE tier_definitions (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)     NOT NULL,
    tier_uid                VARCHAR(128)    NOT NULL,
    name                    VARCHAR(128)    NOT NULL,
    rank_order              INT             NOT NULL,
    entry_threshold         DECIMAL(18,4)   NOT NULL,
    maintenance_threshold   DECIMAL(18,4)   NOT NULL,
    threshold_type          ENUM('LIFETIME_POINTS','ROLLING_12M_SPEND','TRANSACTION_COUNT')
                                NOT NULL,
    points_multiplier       DECIMAL(6,3)    NOT NULL DEFAULT 1.000,
    grace_period_days       INT             NOT NULL DEFAULT 90,
    downgrade_warning_days  INT             NOT NULL DEFAULT 60,
    benefits                JSON            DEFAULT NULL,
    is_invite_only          TINYINT(1)      DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_tenant_tier (tenant_id, tier_uid),
    INDEX idx_tenant_rank (tenant_id, rank_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Undo (manual):
-- DROP TABLE tier_definitions;

