-- Tier change audit log for analytics (cohort upgrade + velocity charts).
-- Apply manually when JPA_DDL_AUTO=validate. Hibernate does not create this table (JDBC-only).

CREATE TABLE IF NOT EXISTS tier_history (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  tenant_id         VARCHAR(64) NOT NULL,
  programme_uid     VARCHAR(64) NOT NULL DEFAULT 'default',
  customer_id       VARCHAR(128) NOT NULL,
  from_tier_uid     VARCHAR(128) NULL,
  to_tier_uid       VARCHAR(128) NOT NULL,
  from_tier_name    VARCHAR(128) NULL,
  to_tier_name      VARCHAR(128) NOT NULL,
  balance_at_change DECIMAL(18, 4) NULL,
  trigger_type      VARCHAR(32) NOT NULL,
  changed_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_tier_history_tenant_programme (tenant_id, programme_uid),
  KEY idx_tier_history_customer (tenant_id, programme_uid, customer_id),
  KEY idx_tier_history_to_tier (tenant_id, programme_uid, to_tier_name),
  KEY idx_tier_history_changed (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
