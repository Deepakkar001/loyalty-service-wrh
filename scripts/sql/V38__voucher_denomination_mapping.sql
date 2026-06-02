-- Phase 1.5: multi-denomination voucher mapping (mixed CSV + points tiers).
-- Run manually when JPA ddl-auto=validate.

CREATE TABLE IF NOT EXISTS voucher_denomination_mapping (
  id                        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  mapping_uid               VARCHAR(128) NOT NULL,
  tenant_id                 VARCHAR(64) NOT NULL,
  catalog_reward_uid        VARCHAR(64) NOT NULL,
  points_required           DECIMAL(18, 4) NOT NULL,
  face_value                DECIMAL(18, 4) NOT NULL,
  currency                  CHAR(3) NOT NULL,
  priority                  INT NOT NULL DEFAULT 0,
  is_active                 TINYINT(1) NOT NULL DEFAULT 1,
  description               VARCHAR(512),
  partner_sku               VARCHAR(128),
  created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_mapping_uid (mapping_uid),
  UNIQUE KEY uk_catalog_points (tenant_id, catalog_reward_uid, points_required, is_active),
  KEY idx_tenant_catalog_active (tenant_id, catalog_reward_uid, is_active),
  KEY idx_catalog_face (catalog_reward_uid, face_value, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE voucher_inventory
  ADD COLUMN denomination_mapping_id BIGINT UNSIGNED NULL AFTER catalog_reward_uid,
  ADD KEY idx_tenant_catalog_face_status (tenant_id, catalog_reward_uid, face_value, status);

ALTER TABLE voucher_batch
  ADD COLUMN metadata_json JSON NULL AFTER error_report_json;
