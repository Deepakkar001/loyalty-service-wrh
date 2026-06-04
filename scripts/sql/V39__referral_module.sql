-- Manual schema change (Flyway not used). Run in MySQL when JPA_DDL_AUTO=validate.
-- Phase 1: referral programme (campaign-style) with code + linking + milestone rewards.

CREATE TABLE IF NOT EXISTS referral_programmes (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  programme_uid             VARCHAR(64) NOT NULL DEFAULT 'default',
  name                      VARCHAR(255) NOT NULL,
  description               TEXT,
  status                    ENUM('ACTIVE','PAUSED','ENDED') NOT NULL DEFAULT 'ACTIVE',
  valid_from                DATETIME(6),
  valid_until               DATETIME(6),
  config_json               JSON NOT NULL,
  max_referrals_per_customer INT NOT NULL DEFAULT 50,
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_tenant_programme (tenant_id, programme_uid),
  KEY idx_tenant_status (tenant_id, status),
  KEY idx_validity (valid_from, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referral_codes (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  programme_uid             VARCHAR(64) NOT NULL DEFAULT 'default',
  customer_id               VARCHAR(128) NOT NULL,
  code                      VARCHAR(64) NOT NULL,
  status                    ENUM('ACTIVE','REVOKED') NOT NULL DEFAULT 'ACTIVE',
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  revoked_at                DATETIME(6),
  UNIQUE KEY uk_tenant_code (tenant_id, code),
  UNIQUE KEY uk_customer_code (tenant_id, programme_uid, customer_id, status),
  KEY idx_customer (tenant_id, programme_uid, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referrals (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  programme_uid             VARCHAR(64) NOT NULL DEFAULT 'default',
  referral_uid              VARCHAR(128) NOT NULL,
  referrer_customer_id      VARCHAR(128) NOT NULL,
  referee_customer_id       VARCHAR(128),
  referral_code_used        VARCHAR(64),
  status                    ENUM('PENDING','SIGNED_UP','REWARDED','FRAUD_FLAGGED','REJECTED') NOT NULL DEFAULT 'PENDING',
  current_stage             INT NOT NULL DEFAULT 0,
  completed_stages_json     JSON,
  purchase_count            INT NOT NULL DEFAULT 0,
  total_spend               DECIMAL(18,4) NOT NULL DEFAULT 0,
  fraud_result_json         JSON,
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_referral_uid (tenant_id, programme_uid, referral_uid),
  UNIQUE KEY uk_referee_once (tenant_id, programme_uid, referee_customer_id),
  KEY idx_referrer (tenant_id, programme_uid, referrer_customer_id, status),
  KEY idx_referee (tenant_id, programme_uid, referee_customer_id, status),
  KEY idx_status (tenant_id, programme_uid, status),
  KEY idx_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referral_rewards_issued (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  programme_uid             VARCHAR(64) NOT NULL DEFAULT 'default',
  referral_uid              VARCHAR(128) NOT NULL,
  stage                     INT NOT NULL,
  recipient_type            ENUM('REFERRER','REFEREE') NOT NULL,
  recipient_customer_id     VARCHAR(128) NOT NULL,
  points_awarded            DECIMAL(18,4) NOT NULL,
  idempotency_key           VARCHAR(128) NOT NULL,
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_idempotency (tenant_id, recipient_customer_id, idempotency_key),
  KEY idx_referral_stage (tenant_id, programme_uid, referral_uid, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS referral_audit_log (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  programme_uid             VARCHAR(64) NOT NULL DEFAULT 'default',
  referral_uid              VARCHAR(128),
  action                    ENUM('CODE_CREATED','CODE_REVOKED','LINKED','STAGE_COMPLETED','REWARD_ISSUED','FRAUD_FLAGGED','REJECTED','OVERRIDE') NOT NULL,
  actor_type                ENUM('SYSTEM','ADMIN','CUSTOMER') NOT NULL DEFAULT 'SYSTEM',
  actor_id                  VARCHAR(128),
  metadata_json             JSON,
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_referral (tenant_id, programme_uid, referral_uid, created_at),
  KEY idx_action (tenant_id, programme_uid, action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

