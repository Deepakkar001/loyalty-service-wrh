-- Merchant onboarding module (manual apply for prod; Hibernate ddl-auto syncs in dev)

CREATE TABLE IF NOT EXISTS merchants (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id                 VARCHAR(64) NOT NULL,
  merchant_uid              VARCHAR(128) NOT NULL,
  legal_name                VARCHAR(255) NOT NULL,
  display_name              VARCHAR(255),
  category                  VARCHAR(128),
  contact_email             VARCHAR(255),
  contact_phone             VARCHAR(20),
  bank_details_vault_ref    VARCHAR(255),
  tax_id                    VARCHAR(128),
  onboarding_stage          VARCHAR(32) NOT NULL DEFAULT 'REGISTRATION',
  earn_rate_multiplier      DECIMAL(6,3) NOT NULL DEFAULT 1.000,
  settlement_cycle          VARCHAR(32) NOT NULL DEFAULT 'MONTHLY',
  commission_config         JSON,
  api_credentials_vault_ref VARCHAR(255),
  agreement_document_url    VARCHAR(512),
  agreement_accepted_at     DATETIME(6),
  integration_test_passed_at DATETIME(6),
  created_by                VARCHAR(255),
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_merchant (tenant_id, merchant_uid),
  KEY idx_tenant_stage (tenant_id, onboarding_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_onboarding_audit (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  audit_uid                 VARCHAR(128) NOT NULL,
  tenant_id                 VARCHAR(64) NOT NULL,
  merchant_uid              VARCHAR(128) NOT NULL,
  from_stage                VARCHAR(64) NOT NULL,
  to_stage                  VARCHAR(64) NOT NULL,
  actor_email               VARCHAR(255) NOT NULL,
  action                    VARCHAR(255) NOT NULL,
  reason                    TEXT,
  metadata_json             JSON,
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_audit_uid (audit_uid),
  KEY idx_merchant (tenant_id, merchant_uid, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_credentials (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  credential_uid            VARCHAR(128) NOT NULL,
  tenant_id                 VARCHAR(64) NOT NULL,
  merchant_uid              VARCHAR(128) NOT NULL,
  username                  VARCHAR(255) NOT NULL,
  password_hash             VARCHAR(255) NOT NULL,
  is_active                 TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at             DATETIME(6),
  login_attempt_count       INT NOT NULL DEFAULT 0,
  locked_until              DATETIME(6),
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_credential_uid (credential_uid),
  UNIQUE KEY uk_username_tenant (username, tenant_id),
  KEY idx_merchant (tenant_id, merchant_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_approval_requests (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  request_uid               VARCHAR(128) NOT NULL,
  tenant_id                 VARCHAR(64) NOT NULL,
  merchant_uid              VARCHAR(128) NOT NULL,
  request_type              VARCHAR(32) NOT NULL,
  payload_json              JSON NOT NULL,
  requested_by              VARCHAR(255) NOT NULL,
  requested_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  reviewed_by               VARCHAR(255),
  reviewed_at               DATETIME(6),
  status                    VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  review_notes              TEXT,
  UNIQUE KEY uk_request_uid (request_uid),
  KEY idx_merchant (tenant_id, merchant_uid),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE campaigns
  ADD COLUMN pending_merchant_approval TINYINT(1) NOT NULL DEFAULT 0;
