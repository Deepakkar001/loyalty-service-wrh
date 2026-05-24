-- LoyaltyOS Integration Module schema (apply manually in production; Hibernate update in dev)
-- MySQL 8.0+

ALTER TABLE tenant_api_keys
  ADD COLUMN IF NOT EXISTS signing_secret_encrypted TEXT NULL COMMENT 'AES-GCM encrypted signing secret',
  ADD COLUMN IF NOT EXISTS rate_limit_requests INT NOT NULL DEFAULT 1000,
  ADD COLUMN IF NOT EXISTS rate_limit_window_minutes INT NOT NULL DEFAULT 60,
  ADD COLUMN IF NOT EXISTS ip_whitelist JSON NULL,
  ADD COLUMN IF NOT EXISTS name VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS description VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS last_secret_revealed_at DATETIME(6) NULL;

CREATE INDEX IF NOT EXISTS idx_tenant_api_keys_tenant_status ON tenant_api_keys(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_tenant_api_keys_created ON tenant_api_keys(tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS api_request_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  api_key_uid VARCHAR(128) NULL,
  request_id VARCHAR(255) NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(500) NOT NULL,
  event_id VARCHAR(255) NULL,
  customer_id VARCHAR(255) NULL,
  http_status INT NOT NULL,
  processing_time_ms INT NOT NULL,
  error_code VARCHAR(50) NULL,
  error_message VARCHAR(500) NULL,
  request_payload_hash CHAR(64) NULL,
  ip_address VARCHAR(45) NOT NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_api_audit_request_id (request_id),
  INDEX idx_api_audit_tenant (tenant_id),
  INDEX idx_api_audit_created (created_at DESC),
  INDEX idx_api_audit_event (event_id),
  INDEX idx_api_audit_status (http_status),
  INDEX idx_api_audit_api_key (api_key_uid)
);

CREATE TABLE IF NOT EXISTS credential_access_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  api_key_uid VARCHAR(128) NOT NULL,
  access_type VARCHAR(32) NOT NULL,
  user_id VARCHAR(255) NULL,
  ip_address VARCHAR(45) NOT NULL,
  reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_credential_access_tenant (tenant_id, created_at DESC),
  INDEX idx_credential_access_key (api_key_uid, created_at DESC)
);

CREATE TABLE IF NOT EXISTS integration_event_processing_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  event_id VARCHAR(255) NOT NULL,
  customer_id VARCHAR(255) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  api_key_uid VARCHAR(128) NULL,
  processing_status VARCHAR(32) NOT NULL,
  http_status INT NOT NULL,
  rules_evaluated_count INT DEFAULT 0,
  rules_matched_count INT DEFAULT 0,
  base_points_calculated DECIMAL(12,2) DEFAULT 0,
  tier_multiplier DECIMAL(4,2) DEFAULT 1.0,
  total_points_awarded DECIMAL(12,2) DEFAULT 0,
  previous_balance DECIMAL(12,2) DEFAULT 0,
  new_balance DECIMAL(12,2) DEFAULT 0,
  tier_before VARCHAR(50) NULL,
  tier_after VARCHAR(50) NULL,
  tier_changed BOOLEAN DEFAULT FALSE,
  campaigns_eligible_count INT DEFAULT 0,
  campaign_bonus_points DECIMAL(12,2) DEFAULT 0,
  processing_time_ms INT NOT NULL,
  error_code VARCHAR(50) NULL,
  error_message VARCHAR(500) NULL,
  request_payload_hash CHAR(64) NOT NULL,
  response_payload_json MEDIUMTEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_event_processing_event (tenant_id, event_id),
  INDEX idx_event_processing_tenant (tenant_id, created_at DESC),
  INDEX idx_event_processing_customer (customer_id),
  INDEX idx_event_processing_status (processing_status)
);
