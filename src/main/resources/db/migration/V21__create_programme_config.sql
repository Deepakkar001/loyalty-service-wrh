-- ─── PROGRAMME CONFIG (VERSIONED BLOB) ───────────────────────
-- Canonical per-programme configuration-as-data (JSON), versioned.
--
-- NOTE: Version chosen to avoid collisions with existing V15/V16 migrations.

CREATE TABLE programme_config (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           VARCHAR(64)     NOT NULL,
    programme_uid       VARCHAR(64)     NOT NULL,
    config_version      INT             NOT NULL,
    config_json         JSON            NOT NULL,
    effective_from      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by_actor_id VARCHAR(64)     DEFAULT NULL,
    created_by_role     VARCHAR(32)     DEFAULT NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_programme_version (tenant_id, programme_uid, config_version),
    INDEX idx_programme_latest (tenant_id, programme_uid, config_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

