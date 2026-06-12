-- Merchant module follow-up: ledger attribution + POS API keys

ALTER TABLE points_ledger
  ADD COLUMN merchant_uid VARCHAR(128) NULL,
  ADD KEY idx_ledger_merchant (merchant_uid);

CREATE TABLE IF NOT EXISTS merchant_api_keys (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  key_uid                   VARCHAR(128) NOT NULL,
  tenant_id                 VARCHAR(64) NOT NULL,
  merchant_uid              VARCHAR(128) NOT NULL,
  key_hash                  VARCHAR(64) NOT NULL,
  key_prefix                VARCHAR(16) NOT NULL,
  name                      VARCHAR(255),
  environment               VARCHAR(32) NOT NULL DEFAULT 'sandbox',
  is_active                 TINYINT(1) NOT NULL DEFAULT 1,
  last_used_at              DATETIME(6),
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_key_uid (key_uid),
  UNIQUE KEY uk_key_hash (key_hash),
  KEY idx_merchant_active (tenant_id, merchant_uid, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
