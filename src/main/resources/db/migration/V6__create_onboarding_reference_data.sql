-- Reference / lookup tables used by onboarding UI
-- Stored in DB so dropdowns can be managed without redeploying.

CREATE TABLE IF NOT EXISTS ref_business_category (
    code        VARCHAR(32)  NOT NULL PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_ref_business_category_label (label),
    INDEX idx_ref_business_category_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ref_country (
    code        CHAR(2)       NOT NULL PRIMARY KEY,
    label       VARCHAR(100)  NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    is_active   TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_ref_country_label (label),
    INDEX idx_ref_country_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed data (idempotent)
INSERT INTO ref_business_category (code, label, sort_order, is_active)
VALUES
  ('RETAIL', 'Retail', 10, 1),
  ('ECOMMERCE', 'E-Commerce', 20, 1),
  ('FINTECH', 'Fintech', 30, 1),
  ('HOSPITALITY', 'Hospitality', 40, 1),
  ('GAMING', 'Gaming', 50, 1),
  ('HEALTHCARE', 'Healthcare', 60, 1),
  ('TELECOM', 'Telecom', 70, 1),
  ('OTHER', 'Other', 80, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  sort_order = VALUES(sort_order),
  is_active = VALUES(is_active);

INSERT INTO ref_country (code, label, sort_order, is_active)
VALUES
  ('IN', 'India', 10, 1),
  ('US', 'United States', 20, 1),
  ('GB', 'United Kingdom', 30, 1),
  ('SG', 'Singapore', 40, 1),
  ('AE', 'UAE', 50, 1),
  ('AU', 'Australia', 60, 1)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  sort_order = VALUES(sort_order),
  is_active = VALUES(is_active);

