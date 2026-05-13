-- ─── PROGRAMMES (MULTI-PROGRAM) ─────────────────────────────
-- A tenant can own multiple loyalty programmes, each with its own config/rules.
--
-- programme_uid is a stable external identifier (UUID-like string).
--
-- NOTE: Version chosen to avoid collisions with existing V15/V16 migrations.

CREATE TABLE programmes (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)     NOT NULL,
    programme_uid           VARCHAR(64)     NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    status                  ENUM('DRAFT','ACTIVE','ARCHIVED')
                                NOT NULL DEFAULT 'DRAFT',
    active_config_version   INT             NOT NULL DEFAULT 0,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_tenant_programme_uid (tenant_id, programme_uid),
    INDEX idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

