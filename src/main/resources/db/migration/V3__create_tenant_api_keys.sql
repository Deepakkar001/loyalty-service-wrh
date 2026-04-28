-- ─── TENANT API KEYS ───────────────────────────────────────
-- SECURITY: Raw API keys are NEVER stored.
-- Only SHA-256 hashes are persisted here.
-- The raw key is returned once at generation and never retrievable again.

CREATE TABLE tenant_api_keys (
    id                      BIGINT UNSIGNED     AUTO_INCREMENT PRIMARY KEY,
    tenant_id               VARCHAR(64)         NOT NULL,
    key_uid                 VARCHAR(128)        NOT NULL,
    key_prefix              VARCHAR(16)         NOT NULL,   -- first 12 chars of key (for lookup)
    key_hash                VARCHAR(64)         NOT NULL,   -- SHA-256(raw_key) hex
    signing_secret_hash     VARCHAR(64)         NOT NULL,   -- SHA-256(raw_secret) hex
    environment             ENUM('SANDBOX','PRODUCTION') NOT NULL,
    status                  ENUM('ACTIVE','REVOKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at              DATETIME(6),
    revoked_at              DATETIME(6),
    last_used_at            DATETIME(6),
    UNIQUE KEY uq_key_uid   (key_uid),
    UNIQUE KEY uq_key_hash  (key_hash),
    INDEX idx_tenant_id     (tenant_id),
    INDEX idx_key_prefix    (key_prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

